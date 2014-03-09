package io.github.alexeygrishin.blockalloc;

import io.github.alexeygrishin.common.Check;

/**
 * Provides access to several small blocks as to single big block.
 */
public class MultiBlockAllocator extends BaseAllocator {

    private RandomAccessAllocator wrapped;
    private int outerBlockSize;
    private int blockInBlocks;

    public MultiBlockAllocator(RandomAccessAllocator wrapped, int outerBlockSize) {
        this.wrapped = wrapped;
        this.outerBlockSize = outerBlockSize;
        Check.arguments(wrapped.getBlockSize() < outerBlockSize, "Outer block size shall be larger than inner block size");
        Check.arguments(outerBlockSize % wrapped.getBlockSize() == 0, "Outer block size shall be factor of inner block size");
        this.blockInBlocks = this.outerBlockSize / wrapped.getBlockSize();
    }

    @Override
    public int getBlockSize() {
        return outerBlockSize;
    }

    @Override
    public int getBlocksCount() {
        return wrapped.getBlocksCount() / blockInBlocks;
    }

    @Override
    public <T> T get(int blockId, Class<T> kls) {
        return wrapped.get(blockId, kls, blockInBlocks);
    }

    @Override
    public void saveModifications(int blockId, Object data) {
        wrapped.saveModifications(blockId, data, blockInBlocks);
    }

    @Override
    public int allocate() {
        return wrapped.allocate(blockInBlocks);
    }

    @Override
    public void free(int blockId) {
        for (int i = 0; i < blockInBlocks; i++) {
            wrapped.free(blockId + i);
        }
    }

    @Override
    public void close() {
        wrapped.close();
    }
}
