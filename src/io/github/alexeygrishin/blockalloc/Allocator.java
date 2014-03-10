package io.github.alexeygrishin.blockalloc;

/**
 * Operates with fixed-size blocks (in memory or on disk), handles their allocation, reusage and fragmentation.
 * Access to different blocks is thread-safe. Access to the same block shall be guarded by the callers.
 */
public interface Allocator {

    /**
     *
     * @return block size in bytes. Const.
     */
    int getBlockSize();

    /**
     *
     * @return amount of existent blocks.
     */
    int getBlocksCount();

    /**
     * Retreive blocks with specified id and "casts" it to the provided class ({@link io.github.alexeygrishin.blockalloc.serializers.Serializer}
     * @param blockId block id, >=0 and < getBlocksCount()
     * @param kls class to load data into.
     * @return object of specified class deserialized from block.
     * @throws java.lang.IndexOutOfBoundsException if trying to read outside storage
     * @throws io.github.alexeygrishin.blockalloc.serializers.SerializationException if cannot convert read bytes to specified class
     * @throws io.github.alexeygrishin.bytestorage.StorageFault
     */
    <T> T get(int blockId, Class<T> kls);

    /**
     * Saves object to the block with specified id.
     * @param blockId block id, >=0 and < getBlocksCount()
     * @param data object to store
     * @throws java.lang.IndexOutOfBoundsException if trying to write outsie storage
     * @throws io.github.alexeygrishin.blockalloc.serializers.SerializationException if cannot convert object to bytes
     **/
    void saveModifications(int blockId, Object data);

    /**
     * Creates new block.
     * Note: actually it may not really create a block but reuse previously freed one.
     * @return block id
     */
    int allocate();

    /**
     * This method allows to perform {@link #get} / {@link #saveModifications} easily with try(resources...) syntax, this way:
     * <code>
     *     try (BlockToModify<MyStruct> block = allocator.getToModify(1, MyStruct.class)) {
     *         block.getBlock().someField = 5;
     *     }
     * </code>
     * instead of
     * <code>
     *     MyStruct struct;
     *     try {
     *          struct = allocator.get(1, MyStruct.class);
     *          struct.someField = 5;
     *     }
     *     finally {
     *          allocator.saveModifications(1, struct);
     *     }
     * </code>
     * @param blockId
     * @param kls
     * @param <T>
     * @return
     */
    <T> BlockToModify<T> getToModify(int blockId, Class<T> kls);

    /**
     * Same as {@link #allocate()} more convenient for try(resources...) syntax
     * @param kls
     * @param <T>
     * @return
     */
    <T> BlockToModify<T> allocateToModify(Class<T> kls);

    /**
     * Informs allocator that provided block is not needed anymore.
     *
     * @param blockId id of block.
     */
    void free(int blockId);

    /**
     * Closes IO storage (if any) and guarantely writes all changes.
     */
    void close();

}
