package io.github.alexeygrishin.blockalloc;

/**
 * See {@link Allocator#getToModify(int, Class)}
 * @param <T>
 */
public class BlockToModify<T> implements AutoCloseable {
    private final Allocator allocator;
    private final int blockId;
    private final T block;

    public BlockToModify(Allocator allocator, int blockId, T block) {
        this.allocator = allocator;
        this.blockId = blockId;
        this.block = block;
    }

    public int getBlockId() {
        return blockId;
    }

    public T getBlock() {
        return block;
    }

    public void close() {
        allocator.saveModifications(blockId, block);
    }
}
