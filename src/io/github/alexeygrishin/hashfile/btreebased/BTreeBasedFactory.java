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
        //TODO: check block ize (shall be > 256 at least);
        try {
            SynchronizedByteContainer container = new FileBytesContainer(FileChannel.open(file.toPath(), StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.CREATE_NEW));
            MetaInformationWrapper wrapper = new MetaInformationWrapper(container);
            MetaInformationWrapper.MetaInfo info =  wrapper.getMetaInfo();
            info.version = VERSION;
            info.blockSize = blockSize;
            info.truncateMethod = part.getValue();
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
        SynchronizedByteContainer container = null;
        try {
            container = new FileBytesContainer(FileChannel.open(file.toPath(), StandardOpenOption.WRITE, StandardOpenOption.READ));
            MetaInformationWrapper wrapper = new MetaInformationWrapper(container);
            MetaInformationWrapper.MetaInfo info =  wrapper.getMetaInfo();
            if (info.version != VERSION) {
                throw new CannotLoadStorage("Version mismatch: file has version " + info.version + ", but expected " + VERSION);
            }
            Allocator allocator = new Cache(new BlockAllocator(wrapper, info.blockSize), DEFAULT_CACHE_SIZE / info.blockSize);
            return new BTreeBasedStorage(allocator, KeyTruncateMethod.valueOf(info.truncateMethod));

        } catch (IOException e) {
            throw new CannotLoadStorage(e);
        }

    }

    @Override
    public NamedStorage create(String filePath) {
        return create(filePath, null, null, null);
    }
}
