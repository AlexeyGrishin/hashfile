package io.github.alexeygrishin.blockalloc;

import io.github.alexeygrishin.bytestorage.SynchronizedByteContainer;
import io.github.alexeygrishin.blockalloc.serializers.Serializer;
import io.github.alexeygrishin.blockalloc.serializers.Serializers;

import java.nio.ByteBuffer;

/**
 * Basic implementation based on {@link io.github.alexeygrishin.bytestorage.SynchronizedByteContainer}.
 */
public class BlockAllocator implements Allocator {

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
        return (int)(byteContainer.getSize() / blockSize);
    }

    private long ptr(int blockId) {
        return blockId * blockSize;
    }

    private int blockId(long ptr) {
        return (int)(ptr / blockSize);
    }

    @Override
    public <T> T get(int blockId, Class<T> kls) {
        ByteBuffer buffer = ByteBuffer.allocate(blockSize);
        byteContainer.read(ptr(blockId), buffer);
        buffer.rewind();
        Serializer<T> serializer = serializers.get(kls, blockSize);
        return serializer.load(buffer);
    }

    @Override
    public void saveModifications(int blockId, Object data) {
        ByteBuffer buffer = ByteBuffer.allocate(blockSize);
        Serializer serializer = serializers.get(data.getClass(), blockSize);
        serializer.save(buffer, data);
        buffer.rewind();
        byteContainer.write(ptr(blockId), buffer);
    }

    @Override
    public <T> BlockToModify<T> getToModify(final int blockId,  Class<T> kls) {
        return new BlockToModify<T>(this, blockId, get(blockId, kls));
    }

    @Override
    public int allocate() {
        ByteBuffer buffer = ByteBuffer.allocate(blockSize);
        long newPtr = byteContainer.append(buffer);
        return blockId(newPtr);
    }

    @Override
    public <T> BlockToModify<T> allocateToModify(Class<T> kls) {
        return getToModify(allocate(), kls);
    }

    @Override
    public void free(int blockId) {
        //do nothing for now
    }

    @Override
    public void close() {
        byteContainer.close();
    }

}
