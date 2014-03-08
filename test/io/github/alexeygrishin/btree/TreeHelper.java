package io.github.alexeygrishin.btree;

import io.github.alexeygrishin.blockalloc.Allocator;
import io.github.alexeygrishin.blockalloc.BlockAllocator;
import io.github.alexeygrishin.bytestorage.MemoryContainer;
import io.github.alexeygrishin.bytestorage.SynchronizedByteContainer;

import java.util.*;

import static io.github.alexeygrishin.tool.TestTool.iteratorToList;
import static org.junit.Assert.assertEquals;

public class TreeHelper {

    public static BTree createTree(int t) {
        MemoryContainer ctr = new MemoryContainer();
        return createTree(t, ctr);
    }

    public static BTree createTree(int t, SynchronizedByteContainer ctr) {
        Allocator storage = new BlockAllocator(ctr, blockSize(t));
        return new BTree(storage);
    }

    public static int blockSize(int t) {
        return (t*2)*256;
    }

    public static void assertKeys(BTree tree, String... expectedKeys) {
        List<String> expected = new ArrayList<>(Arrays.asList(expectedKeys));
        List<String> actual = iteratorToList(tree);
        assertListsEqual(expected, actual);
    }

    public static void assertListsEqual(List<? extends Comparable> expected, List<? extends Comparable> actual) {
        Collections.sort(expected);
        Collections.sort(actual);
        assertEquals(expected, actual);
    }

    public static void assertListsEqual(String message, List<? extends Comparable> expected, List<? extends Comparable> actual) {
        Collections.sort(expected);
        Collections.sort(actual);
        assertEquals(message, expected, actual);
    }

}
