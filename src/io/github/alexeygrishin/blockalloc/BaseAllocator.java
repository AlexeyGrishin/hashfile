package io.github.alexeygrishin.blockalloc;

public abstract class BaseAllocator implements Allocator {
    @Override
    public <T> BlockToModify<T> getToModify(final int blockId,  Class<T> kls) {
        return new BlockToModify<T>(this, blockId, get(blockId, kls));
    }

    @Override
    public <T> BlockToModify<T> allocateToModify(Class<T> kls) {
        return getToModify(allocate(), kls);
    }
}
