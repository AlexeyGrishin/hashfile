package io.github.alexeygrishin.hashfile.btreebased;

import static io.github.alexeygrishin.common.Check.*;

import io.github.alexeygrishin.blockalloc.*;
import io.github.alexeygrishin.bytestorage.FileBytesContainer;
import io.github.alexeygrishin.bytestorage.SynchronizedByteContainer;
import io.github.alexeygrishin.btree.KeyTruncateMethod;
import io.github.alexeygrishin.hashfile.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

/**
 * Produces B-tree named storage, defines defaults.
 * Block size for B-tree is selected automatically to be >= 256K
 */
public class BTreeBasedFactory implements NamedStorageFactory {

    public static final int VERSION = 0x03;
    public static final int DEFAULT_TREE_BLOCK_SIZE_KB = 256;
    public static final int DEFAULT_DATA_BLOCK_SIZE_KB = 4;
    public final static int DEFAULT_CACHE_SIZE_MB = 64;
    public static final int KB = 1024;
    public static final int MB = 1024 * 1024;

    @Override
    public NamedStorage create(String filePath, Integer dataBlockSizeK, Integer cacheSizeM, KeyTruncateMethod part) {
        File file = new File(filePath);
        if (file.exists()) {
            throw new CannotCreateStorage("There is already file on path '" + filePath + "'");
        }
        int dataBlockSize = KB * positive(notNull(dataBlockSizeK, DEFAULT_DATA_BLOCK_SIZE_KB), "dataBlockSize");
        int treeBlockSize = Math.max(dataBlockSize, DEFAULT_TREE_BLOCK_SIZE_KB * KB);
        if (treeBlockSize > dataBlockSize && treeBlockSize % dataBlockSize != 0) {
            treeBlockSize = (treeBlockSize / dataBlockSize + 1) * dataBlockSize;
        }
        cacheSizeM = MB * positive(notNull(cacheSizeM, DEFAULT_CACHE_SIZE_MB), "cacheLimit");
        part = notNull(part, KeyTruncateMethod.LEADING);
        return createStorage(file, treeBlockSize, dataBlockSize, cacheSizeM, part);
    }

    private BTreeBasedStorage createStorage(File file, int treeBlockSize, int dataBlockSize, Integer cacheSize, KeyTruncateMethod part) {
        try {
            SynchronizedByteContainer container = createBytesContainer(file);
            MetaInformationWrapper wrapper = new MetaInformationWrapper(container);
            MetaInformationWrapper.MetaInfo info =  wrapper.getMetaInfo();
            info.version = VERSION;
            info.treeBlockSize = treeBlockSize;
            info.dataBlockSize = dataBlockSize;
            info.truncateMethod = part.getValue();
            info.cacheSize = cacheSize;
            wrapper.setMetaInfo(info);

            BlockAllocator allocator = new BlockAllocator(wrapper, dataBlockSize);
            Cache cache = new Cache(allocator, cacheSize);
            Allocator treeAllocator = createTreeAllocator(info, cache);
            return new BTreeBasedStorage(treeAllocator, cache, part);
        } catch (IOException e) {
            throw new CannotCreateStorage(e);
        }
    }

    protected SynchronizedByteContainer createBytesContainer(File file) throws IOException {
        return new FileBytesContainer(FileChannel.open(file.toPath(), StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.CREATE_NEW));
    }

    private Allocator createTreeAllocator(MetaInformationWrapper.MetaInfo info, Cache dataAllocator) {
        return info.treeBlockSize > info.dataBlockSize ? new MultiBlockAllocator(dataAllocator, info.treeBlockSize) : dataAllocator;
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
        BlockAllocator allocator = new BlockAllocator(wrapper, info.dataBlockSize);
        Cache dataAllocator = new Cache(allocator, info.cacheSize);
        Allocator treeAllocator = createTreeAllocator(info, dataAllocator);
        return new BTreeBasedStorage(treeAllocator, dataAllocator, KeyTruncateMethod.valueOf(info.truncateMethod));
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
        File file = getExistentFile(filePath);
        try {
            MetaInformationWrapper wrapper = openFile(file);
            MetaInformationWrapper.MetaInfo info =  wrapper.getMetaInfo();
            checkVersion(info);
            File tempFile = getTempFileIn(file.getParentFile());
            try (BTreeBasedStorage copy =  createStorage(tempFile, info.treeBlockSize, info.dataBlockSize, info.cacheSize, KeyTruncateMethod.valueOf(info.truncateMethod));
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

    private File getExistentFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new CannotLoadStorage("File does not exist: " + file.getAbsolutePath());
        }
        return file;
    }

    @Override
    public void printInfo(String filePath, PrintStream out) {
        File file = getExistentFile(filePath);
        try {
            MetaInformationWrapper wrapper = openFile(file);
            MetaInformationWrapper.MetaInfo info =  wrapper.getMetaInfo();
            out.println("BTree based storage");
            out.println(String.format("  Version: %d", info.version));
            out.println("  Block size: ");
            out.println(String.format("    Data: %d K", info.dataBlockSize / 1024));
            out.println(String.format("    Tree: %d K", info.treeBlockSize/ 1024));
            out.println(String.format("  Cache: %d M", info.cacheSize / 1024 / 1024));
            out.println(String.format("  Truncate method: %s", KeyTruncateMethod.valueOf(info.truncateMethod).toString().toLowerCase()));

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
