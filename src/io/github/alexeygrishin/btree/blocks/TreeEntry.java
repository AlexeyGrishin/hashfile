package io.github.alexeygrishin.btree.blocks;

import io.github.alexeygrishin.blockalloc.serializers.Limited;
import io.github.alexeygrishin.btree.BTree;
import io.github.alexeygrishin.btree.TreeNameHelper;
import io.github.alexeygrishin.common.Pointer;

@Limited(size = BTree.ENTRY_SIZE)
public class TreeEntry {
    public int hash;
    public int childPtr;
    public int keyLen;

    @Limited(size = BTree.KEY_PART_SIZE)
    public String keyPart;
    public long data;

    public TreeEntry() {
    }



    public TreeEntry(String key, int hash, long data, TreeNameHelper helper, int childPtr) {
        this.keyLen = key.length();
        this.hash = hash;
        this.keyPart = helper.truncate(key, BTree.KEY_PART_LENGTH) ;
        this.data = data;
        this.childPtr = childPtr;
    }

    public boolean hasChildren() {
        return Pointer.isValid(childPtr);
    }

    public boolean isBroken() {
        return keyLen == 0 && childPtr == 0 && data == 0;
    }

    public boolean isWholeKey() {
        return keyLen == keyPart.length();
    }

}
