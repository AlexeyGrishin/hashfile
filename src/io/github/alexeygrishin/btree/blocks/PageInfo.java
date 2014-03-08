package io.github.alexeygrishin.btree.blocks;

import io.github.alexeygrishin.blockalloc.serializers.Limited;
import io.github.alexeygrishin.btree.BTree;

import static io.github.alexeygrishin.common.Pointer.isValid;

@Limited(size = BTree.ENTRY_SIZE)
public class PageInfo {
    public int countOfEntries;
    public int lastChildPtr;

    public boolean hasLastChild() {
        return isValid(lastChildPtr);
    }
}
