package io.github.alexeygrishin.btree;

import io.github.alexeygrishin.TestBaseWithCounter;
import io.github.alexeygrishin.blockalloc.Allocator;
import io.github.alexeygrishin.blockalloc.BlockAllocator;
import io.github.alexeygrishin.hashfile.btreebased.DataStorage;
import org.junit.Before;
import org.junit.Test;

import java.io.*;

import static org.junit.Assert.*;

public class DataStorageTest extends TestBaseWithCounter {

    private Allocator allocator;
    private DataStorage storage;
    private ByteArrayOutputStream out;

    @Before
    public void setup() {
        allocator = new BlockAllocator(counter, 22);    //so there is 5 bytes for data
        storage = new DataStorage(allocator);
        out = new ByteArrayOutputStream();

    }

    private InputStream str(String str) {
        return new ByteArrayInputStream(str.getBytes());
    }

    private String str(ByteArrayOutputStream out) {
        return new String(out.toByteArray());
    }

    @Test
    public void saveGetName_short() {
        int blockId = storage.insert("test", str(""));
        assertEquals("test", storage.getFullName(blockId));
        assertReadsWrites(2, 1, 1);
    }

    @Test
    public void saveGetName_wholePage() {
        int blockId = storage.insert("test1", str(""));
        assertEquals("test1", storage.getFullName(blockId));
        assertReadsWrites(2, 1, 1);
    }

    @Test
    public void saveGetName_2pages() {
        int blockId = storage.insert("test12", str(""));
        assertEquals("test12", storage.getFullName(blockId));
        assertReadsWrites(4, 2, 2);
    }

    @Test
    public void saveGetName_3pages() {
        int blockId = storage.insert("....$....$...", str(""));
        assertEquals("....$....$...", storage.getFullName(blockId));
        assertReadsWrites(6, 3, 3);
    }

    @Test
    public void saveGetData_empty() {
        int blockId = storage.insert("abc", str(""));
        storage.select(blockId, out);
        assertEquals("", str(out));
    }


    @Test
    public void saveGetData_samePageWithName() {
        int blockId = storage.insert("abc", str("d"));
        assertEquals("abc", storage.getFullName(blockId));
        storage.select(blockId, out);
        assertEquals("d", str(out));
        assertReadsWrites(4, 1, 1);
    }

    @Test
    public void saveGetData_startsFromAnotherPage() {
        int blockId = storage.insert("abc11", str("d2"));
        assertEquals("abc11", storage.getFullName(blockId));
        storage.select(blockId, out);
        assertEquals("d2", str(out));
    }

    @Test
    public void saveGetData_2pages() {
        int blockId = storage.insert("abc11", str("1111122222"));
        assertEquals("abc11", storage.getFullName(blockId));
        storage.select(blockId, out);
        assertEquals("1111122222", str(out));
    }

    @Test
    public void saveGetName_empty() {
        int blockId = storage.insert("", str(""));
        assertEquals("", storage.getFullName(blockId));
        assertReadsWrites(2, 1, 1);
    }

    @Test
    public void updateData_sameSize() {
        int blockId = storage.insert("key", str("value"));
        storage.update(blockId, str("eulav"));
        storage.select(blockId, out);
        assertEquals("eulav", str(out));
    }

    @Test
    public void updateData_larger() {
        int blockId = storage.insert("key", str("value"));
        storage.update(blockId, str("anothervalue"));
        storage.select(blockId, out);
        assertEquals("anothervalue", str(out));
    }

    @Test
    public void updateData_smaller() {
        int blockId = storage.insert("key", str("value"));
        storage.update(blockId, str("?"));
        storage.select(blockId, out);
        assertEquals("?", str(out));
    }

    @Test(expected = NoSuchElement.class)
    public void delete_getName() {
        int blockId = storage.insert("key", str("value"));
        storage.delete(blockId);
        storage.getFullName(blockId);
    }

    @Test(expected = NoSuchElement.class)
    public void delete_getData() {
        int blockId = storage.insert("key", str("value"));
        storage.delete(blockId);
        storage.select(blockId, out);
    }
}
