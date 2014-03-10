package io.github.alexeygrishin.btree;

/**
 * Provides data pointer to insert into BTree entry
 */
public interface TreeData {
    /**
     * Called when entry is inserted into BTree, so there was no entry for provided key
     * @return new data pointer
     */
    public long createData();

    /**
     * Called when entry already exists in BTree
     * @param oldData previous data pointer
     * @return new data pointer to replace old one (may be the same)
     */
    public long updateData(long oldData);
}
