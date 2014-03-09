package io.github.alexeygrishin.hashfile.btreebased;

import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;
import io.github.alexeygrishin.blockalloc.Allocator;
import io.github.alexeygrishin.btree.*;
import io.github.alexeygrishin.common.Pointer;
import io.github.alexeygrishin.hashfile.NamedStorage;

import java.io.*;
import java.util.Iterator;

import static io.github.alexeygrishin.common.Check.safeInt;

/**
 * Named storage based on B-Tree. B-tree is used to quickly find/insert/delete data by name.
 *
 * May use different storages for B-tree and data, may use the same. But it is recommented that
 * block size fo B-tree shall be at least 1MB, so you may use {@link io.github.alexeygrishin.blockalloc.MultiBlockAllocator}
 * over allocator with smaller block size.
 */
public class BTreeBasedStorage implements NamedStorage {

    public static final int NAMES_CACHE_SIZE = 1024 * 1024;
    public static final int NAMES_CACHE_INITIAL_COUNT = 1024;
    private BTree tree;
    private DataStorage storage;

    /**
     * Creates storage
     * @param treeAllocator allocator for B-Tree data, block size shall be >> 256 bytes
     * @param dataAllocator allocator for user data, block size shall be ~ average data size. May be same as treeAllocator.
     * @param truncateMethod how to truncate long keys for comparison
     */
    public BTreeBasedStorage(Allocator treeAllocator, Allocator dataAllocator, final KeyTruncateMethod truncateMethod) {
        this.storage = new DataStorage(dataAllocator);
        this.tree = new BTree(treeAllocator, new TreeNamesCache(new TreeNameHelper() {
            @Override
            public String getFullName(long dataId) {
                return storage.getFullName(safeInt(dataId));
            }

            @Override
            public String truncate(String fullName, int targetLen) {
                return Truncate.part(fullName, targetLen, truncateMethod);
            }
        }, NAMES_CACHE_SIZE, NAMES_CACHE_INITIAL_COUNT));
    }

    @Override
    public boolean getInto(String key, OutputStream stream) {
        long pointer = tree.get(key);
        if (!Pointer.isValid(pointer)) return false;
        storage.select(safeInt(pointer), stream);
        return true;
    }

    @Override
    public void saveFrom(final String key, final InputStream stream) {
        this.tree.put(key, new TreeData() {
            @Override
            public long createData() {
                return storage.insert(key, stream);
            }

            @Override
            public long updateData(long oldData) {
                storage.update(safeInt(oldData), stream);
                return oldData;
            }
        });
    }

    @Override
    public boolean contains(String key) {
        return tree.contains(key);
    }

    @Override
    public void delete(String key) {
        long data = tree.remove(key);
        if (data != -1) {
            storage.delete(safeInt(data));
        }
    }

    @Override
    public Iterator<String> iterator() {
        return tree.iterator();
    }

    @Override
    public void close() {
        tree.close();
        storage.close();
    }

    @Override
    public void cloneTo(NamedStorage storage) {
        for (String key: tree) {
            ByteArrayOutputStream bstream = new ByteArrayOutputStream();
            getInto(key, bstream);
            byte[] bytes = bstream.toByteArray();
            storage.saveFrom(key, new ByteInputStream(bytes, bytes.length));
        }

    }
}
