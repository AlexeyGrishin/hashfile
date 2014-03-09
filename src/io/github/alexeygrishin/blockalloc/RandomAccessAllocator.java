package io.github.alexeygrishin.blockalloc;

/**
 * For internal usage. Provides ability to operate several blocks at once
 */
interface RandomAccessAllocator extends Allocator {

    /**
     * Allocates specified number of blocks. Guaranteed that allocated blocks will follow each other in byte storage, i.e.
     * after calling int blockId = allocate(4) it will be possible to get all blocks separately with get(blockId+N) where 0<=N<=3.
     * @return index of first allocated block
     * @see io.github.alexeygrishin.blockalloc.Allocator#allocate()
     */
    int allocate(int blocks);

    /**
     * Gets several blocks at once starting from blockId and casts it to the specified class.
     * @see io.github.alexeygrishin.blockalloc.Allocator#get(int, Class)
     */
    <T> T get(int blockId, Class<T> kls, int blocks);

    /**
     * Saves object into the several blocks starting from blockId.
     * @see Allocator#saveModifications(int, Object)
     */
    void saveModifications(int blockId, Object data, int blocks);

}
