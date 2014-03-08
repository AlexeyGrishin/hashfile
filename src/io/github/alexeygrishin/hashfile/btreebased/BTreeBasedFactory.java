package io.github.alexeygrishin.hashfile.btreebased;

import static io.github.alexeygrishin.common.Check.*;

import io.github.alexeygrishin.blockalloc.Allocator;
import io.github.alexeygrishin.blockalloc.BlockAllocator;
import io.github.alexeygrishin.blockalloc.Cache;
import io.github.alexeygrishin.bytestorage.FileBytesContainer;
import io.github.alexeygrishin.bytestorage.SynchronizedByteContainer;
import io.github.alexeygrishin.btree.KeyTruncateMethod;
import io.github.alexeygrishin.hashfile.*;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

public class BTreeBasedFactory implements NameBasedStorageFactory {

    public static final int VERSION = 0x01;
    public static final int DEFAULT_BLOCK_SIZE = 1024;
    public final static int DEFAULT_CACHE_SIZE = 1024*1024*64;

    @Override
    public NamedStorage create(String filePath, Integer blockSizeK, Integer cacheLimit, KeyTruncateMethod part) {
        File file = new File(filePath);
        if (file.exists()) {
            throw new CannotCreateStorage("There is already file on path '" + filePath + "'");
        }
        int blockSize = 1024 * positive(notNull(blockSizeK, DEFAULT_BLOCK_SIZE), "blockSize");
        cacheLimit = positive(notNull(cacheLimit, DEFAULT_CACHE_SIZE / blockSize), "cacheLimit");
        part = notNull(part, KeyTruncateMethod.LEADING);
        return createStorage(file, blockSize, cacheLimit, part);
    }

    private BTreeBasedStorage createStorage(File file, int blockSize, Integer cacheLimit, KeyTruncateMethod part) {
        try {
            SynchronizedByteContainer container = new FileBytesContainer(FileChannel.open(file.toPath(), StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.CREATE_NEW));
            MetaInformationWrapper wrapper = new MetaInformationWrapper(container);
            MetaInformationWrapper.MetaInfo info =  wrapper.getMetaInfo();
            info.version = VERSION;
            info.blockSize = blockSize;
            info.truncateMethod = part.getValue();
            info.cacheSize = cacheLimit;
            wrapper.setMetaInfo(info);
            Allocator allocator = new Cache(new BlockAllocator(wrapper, blockSize), cacheLimit);
            return new BTreeBasedStorage(allocator, part);
        } catch (IOException e) {
            throw new CannotCreateStorage(e);
        }
    }

    @Override
    public NamedStorage load(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return create(filePath);
        }
        try {
            MetaInformationWrapper wrapper = openFile(file);
            MetaInformationWrapper.MetaInfo info =  wrapper.getMetaInfo();
            checkVersion(info);
            return loadStorage(wrapper, info);

        } catch (IOException e) {
            throw new CannotLoadStorage(e);
        }

    }

    private BTreeBasedStorage loadStorage(MetaInformationWrapper wrapper, MetaInformationWrapper.MetaInfo info) {
        Allocator allocator = new Cache(new BlockAllocator(wrapper, info.blockSize), info.cacheSize);
        return new BTreeBasedStorage(allocator, KeyTruncateMethod.valueOf(info.truncateMethod));
    }

    private void checkVersion(MetaInformationWrapper.MetaInfo info) {
        if (info.version != VERSION) {
            throw new CannotLoadStorage("Version mismatch: file has version " + info.version + ", but expected " + VERSION);
        }
    }

    private MetaInformationWrapper openFile(File file) throws IOException {
        SynchronizedByteContainer container;
        container = new FileBytesContainer(FileChannel.open(file.toPath(), StandardOpenOption.WRITE, StandardOpenOption.READ));
        return new MetaInformationWrapper(container);
    }

    public void truncate(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new CannotLoadStorage("File does not exist: " + file.getAbsolutePath());
        }
        try {
            MetaInformationWrapper wrapper = openFile(file);
            MetaInformationWrapper.MetaInfo info =  wrapper.getMetaInfo();
            checkVersion(info);
            File tempFile = getTempFileIn(file.getParentFile());
            try (BTreeBasedStorage copy =  createStorage(tempFile, info.blockSize, info.cacheSize, KeyTruncateMethod.valueOf(info.truncateMethod));
                BTreeBasedStorage original = loadStorage(wrapper, info)) {
                original.cloneTo(copy);
            }
            File beforeDelete = getTempFileIn(file.getParentFile());
            if (!file.renameTo(beforeDelete)) {
                throw new CannotLoadStorage("Cannot rename original file to " + beforeDelete.getName());
            }
            if (!tempFile.renameTo(file)) {
                beforeDelete.renameTo(file);
                throw new CannotLoadStorage("Cannot rename truncated file to " + file.getName());
            }
            beforeDelete.delete();

        } catch (IOException e) {
            throw new CannotLoadStorage(e);
        }

    }

    private File getTempFileIn(File directory) {
        File tempFile;
        int idx = 0;
        do {
            tempFile = new File(directory, "_truncate" + idx);
            idx++;
        } while (tempFile.exists());
        return tempFile;
    }

    @Override
    public NamedStorage create(String filePath) {
        return create(filePath, null, null, null);
    }
}
