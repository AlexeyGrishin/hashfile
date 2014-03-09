package io.github.alexeygrishin.blockalloc;

import io.github.alexeygrishin.bytestorage.SynchronizedByteContainer;
import io.github.alexeygrishin.blockalloc.serializers.Serializer;
import io.github.alexeygrishin.blockalloc.serializers.Serializers;
import io.github.alexeygrishin.common.Check;

import java.nio.ByteBuffer;

import static io.github.alexeygrishin.common.Check.safeInt;

/**
 * Basic implementation based on {@link io.github.alexeygrishin.bytestorage.SynchronizedByteContainer}.
 */
public class BlockAllocator extends BaseAllocator implements RandomAccessAllocator {

    private SynchronizedByteContainer byteContainer;
    private int blockSize;
    private Serializers serializers = new Serializers();

    public BlockAllocator(SynchronizedByteContainer byteContainer, int blockSize) {
        this.byteContainer = byteContainer;
        this.blockSize = blockSize;
    }

    @Override
    public int getBlockSize() {
        return blockSize;
    }

    @Override
    public int getBlocksCount() {
        return safeInt(byteContainer.getSize() / blockSize);
    }

    private long ptr(int blockId) {
        return (long)blockId * blockSize;
    }

    private int blockId(long ptr) {
        return safeInt(ptr / blockSize);
    }

    @Override
    public <T> T get(int blockId, Class<T> kls) {
        return get(blockId, 0, kls, blockSize);
    }

    private <T> T get(int blockId, int offset, Class<T> kls, int blockSize) {
        ByteBuffer buffer = ByteBuffer.allocate(blockSize);
        byteContainer.read(ptr(blockId) + offset, buffer);
        buffer.rewind();
        Serializer<T> serializer = serializers.get(kls, blockSize);
        return serializer.load(buffer);

    }

    @Override
    public void saveModifications(int blockId, Object data) {
        saveModifications(blockId, 0, data, blockSize);
    }

    @SuppressWarnings("unchecked")
    private void saveModifications(int blockId, int offset, Object data, int blockSize) {
        ByteBuffer buffer = ByteBuffer.allocate(blockSize);
        Serializer serializer = serializers.get(data.getClass(), blockSize);
        serializer.save(buffer, data);
        buffer.rewind();
        byteContainer.write(ptr(blockId) + offset, buffer);
    }

    @Override
    public int allocate() {
        return allocateBlock(blockSize);
    }

    private int allocateBlock(int blockSize) {
        ByteBuffer buffer = ByteBuffer.allocate(blockSize);
        long newPtr = byteContainer.append(buffer);
        return blockId(newPtr);
    }

    @Override
    public void free(int blockId) {
        //do nothing for now
    }

    @Override
    public void close() {
        byteContainer.close();
    }

    @Override
    public int allocate(int blocks) {
        Check.positive(blocks, "blocks count");
        return allocateBlock(blocks * blockSize);
    }

    @Override
    public <T> T get(int blockId, Class<T> kls, int blocks) {
        Check.positive(blocks, "blocks count");
        return get(blockId, 0, kls, blocks * blockSize);
    }

    @Override
    public void saveModifications(int blockId, Object data, int blocks) {
        Check.positive(blocks, "blocks count");
        saveModifications(blockId, 0, data, blocks * blockSize);
    }
}
