package io.github.alexeygrishin.btree.blocks;

import io.github.alexeygrishin.blockalloc.serializers.DynamicallySized;
import io.github.alexeygrishin.blockalloc.serializers.Serializer;
import io.github.alexeygrishin.blockalloc.serializers.Serializers;
import io.github.alexeygrishin.blockalloc.serializers.StringSerializer;
import io.github.alexeygrishin.btree.BTree;
import io.github.alexeygrishin.common.Pointer;

import java.nio.ByteBuffer;


public class Page implements Serializer<Page>, DynamicallySized {
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

    @Override
    //Manual serialization for better performance
    public void save(ByteBuffer buffer, Page instance) {
        buffer.putInt(instance.pageInfo.countOfEntries);
        buffer.putInt(instance.pageInfo.lastChildPtr);
        StringSerializer ser = new StringSerializer(BTree.KEY_PART_SIZE);
        for (int i = 0; i < instance.pageInfo.countOfEntries; i++) {
            buffer.position(BTree.ENTRY_SIZE * i + BTree.ENTRY_SIZE);
            TreeEntry entry = instance.entries[i];
            buffer.putInt(entry.hash);
            buffer.putInt(entry.childPtr);
            buffer.putInt(entry.keyLen);
            ser.save(buffer, entry.keyPart);
            buffer.putLong(entry.data);
        }
        buffer.position(size);
    }

    @Override
    public Page load(ByteBuffer buffer) {
        Page page = new Page();
        page.pageInfo = new PageInfo();
        page.pageInfo.countOfEntries = buffer.getInt();
        page.pageInfo.lastChildPtr = buffer.getInt();
        StringSerializer ser = new StringSerializer(BTree.KEY_PART_SIZE);
        page.entries = new TreeEntry[entriesMaxCount];
        //TODO[performance]: as BTree uses binary search for most of operations there is no need to
        //deserialize all TreeEntries. They could be unserialized lazily by request.
        for (int i = 0; i < page.pageInfo.countOfEntries; i++) {
            buffer.position(BTree.ENTRY_SIZE * i + BTree.ENTRY_SIZE);
            TreeEntry entry = new TreeEntry();
            entry.hash = buffer.getInt();
            entry.childPtr = buffer.getInt();
            entry.keyLen = buffer.getInt();
            entry.keyPart = ser.load(buffer);
            entry.data = buffer.getLong();
            page.entries[i] = entry;
        }
        buffer.position(size);
        return page;
    }

    @Override
    public int getSize() {
        return size;
    }

    private int size;
    private int entriesMaxCount;

    @Override
    public void setSize(int size) {
        this.size = size;
        entriesMaxCount = (size - BTree.ENTRY_SIZE) / BTree.ENTRY_SIZE;
    }
}
