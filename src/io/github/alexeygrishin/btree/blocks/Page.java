package io.github.alexeygrishin.btree.blocks;

import io.github.alexeygrishin.common.Pointer;

public class Page {
    public PageInfo pageInfo;
    public TreeEntry[] entries;

    public int getCount() {
        return pageInfo.countOfEntries;
    }

    public int nextChild(int index) {
        return child(index + 1);
    }

    public void setNextChild(int index, int child) {
        setChild(index + 1, child);
    }

    public int child(int index) {
        if (index > pageInfo.countOfEntries) {
            return Pointer.NULL_PTR;
        }
        else if (index == pageInfo.countOfEntries) {
            return pageInfo.lastChildPtr;
        }
        else {
            return entries[index].childPtr;
        }
    }

    public void setChild(int index, int child) {
        if (index == pageInfo.countOfEntries) {
            pageInfo.lastChildPtr = child;
        }
        else {
            entries[index].childPtr = child;
        }
    }
}
