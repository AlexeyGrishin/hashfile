package io.github.alexeygrishin.btree.blocks;

import io.github.alexeygrishin.common.Pointer;

public class Page {
    public PageInfo pageInfo;
    public TreeEntry[] entries;

    public int getCount() {
        return pageInfo.countOfEntries;
    }

    public int nextChild(int index) {
        if (index + 1 > pageInfo.countOfEntries) {
            return Pointer.NULL_PTR;
        }
        else if (index + 1 == pageInfo.countOfEntries) {
            return pageInfo.lastChildPtr;
        }
        else {
            return entries[index + 1].childPtr;
        }
    }

    public void setNextChild(int index, int child) {
        if (index + 1 == pageInfo.countOfEntries) {
            pageInfo.lastChildPtr = child;
        }
        else {
            entries[index + 1].childPtr = child;
        }

    }

    public int child(int index) {
        return nextChild(index - 1);    //TODO
    }

    public void setChild(int index, int childPagePtr) {
        setNextChild(index-1, childPagePtr);    //TODO
    }
}
