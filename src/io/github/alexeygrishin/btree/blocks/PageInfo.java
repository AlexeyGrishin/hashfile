package io.github.alexeygrishin.btree.blocks;

import io.github.alexeygrishin.blockalloc.serializers.Limited;

import static io.github.alexeygrishin.common.Pointer.isValid;

@Limited(size = 256)
public class PageInfo {
    public int countOfEntries;
    public int lastChildPtr;

    public boolean hasLastChild() {
        return isValid(lastChildPtr);
    }
}
