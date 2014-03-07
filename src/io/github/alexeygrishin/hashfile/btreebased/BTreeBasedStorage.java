package io.github.alexeygrishin.hashfile.btreebased;

import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;
import io.github.alexeygrishin.blockalloc.Allocator;
import io.github.alexeygrishin.btree.*;
import io.github.alexeygrishin.common.Pointer;
import io.github.alexeygrishin.hashfile.NamedStorage;

import java.io.*;
import java.util.Iterator;

public class BTreeBasedStorage implements NamedStorage {

    public static final int NAMES_CACHE_SIZE = 1024 * 1024;
    public static final int NAMES_CACHE_INITIAL_COUNT = 1024;
    private BTree tree;
    private DataStorage storage;

    public BTreeBasedStorage(Allocator allocator, final KeyTruncateMethod part) {
        this.storage = new DataStorage(allocator);
        this.tree = new BTree(allocator, new TreeNamesCache(new TreeNameHelper() {
            @Override
            public String getFullName(int dataId) {
                return storage.getFullName(dataId);
            }

            @Override
            public String truncate(String fullName, int targetLen) {
                return Truncate.part(fullName, targetLen, part);
            }
        }, NAMES_CACHE_SIZE, NAMES_CACHE_INITIAL_COUNT));
    }

    @Override
    public boolean getInto(String key, OutputStream stream) {
        long pointer = tree.get(key);
        if (!Pointer.isValid(pointer)) return false;
        storage.select((int)pointer, stream);
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
                storage.update((int)oldData, stream);
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
            storage.delete((int)data);
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
