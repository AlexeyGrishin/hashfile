package io.github.alexeygrishin.btree;

import io.github.alexeygrishin.blockalloc.BlockAllocator;
import io.github.alexeygrishin.bytestorage.Counter;
import io.github.alexeygrishin.bytestorage.MemoryContainer;
import io.github.alexeygrishin.tool.TestTool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static io.github.alexeygrishin.btree.TreeHelper.*;
import static org.junit.Assert.*;

public class BTreeTest {


    public static class EmptyTree {
        @Test
        public void getSize() {
            assertEquals(0, createTree(3).size());
        }

        @Test
        public void iterator_hasNext() {
            Iterator<String> iter = createTree(3).iterator();
            assertFalse(iter.hasNext());
        }

        @Test(expected = NoSuchElementException.class)
        public void iterator_next() {
            Iterator<String> iter = createTree(3).iterator();
            iter.next();
        }

        @Test
        public void contains() {
            assertFalse(createTree(3).contains("a"));
        }
    }

    public static class PutWithData {
        private BTree tree;

        @Before
        public void setup() {
            tree = createTree(3);
        }

        @Test
        public void whenCreate() {
            tree.put("1", new TreeData() {
                @Override
                public long createData() {
                    return 10;
                }

                @Override
                public long updateData(long oldData) {
                    fail("Called updateData instead of createData");
                    return 0;
                }
            });
        }

        @Test
        public void whenUpdate() {
            tree.put("1", 12);
            tree.put("1", new TreeData() {
                @Override
                public long createData() {
                    fail("Called createData instead of updateData");
                    return 1;
                }

                @Override
                public long updateData(long oldData) {
                    assertEquals(12, oldData);
                    return 11;
                }
            });
        }
    }

    public static class SingleValue {

        private BTree tree;

        @Before
        public void setup() {
            tree = createTree(3);
            tree.put("a", 42);
        }

        @Test
        public void getSize() {
            assertEquals(1, tree.size());
        }

        @Test
        public void get() {
            assertEquals(42, tree.get("a"));
        }

        @Test
        public void contains() {
            assertTrue(tree.contains("a"));
        }

        @Test
        public void iterator() {
            assertKeys(tree, "a");
        }

        @Test
        public void update() {
            tree.put("a", 451);
            assertEquals(1, tree.size());
            assertEquals(451, tree.get("a"));
            assertKeys(tree, "a");
        }

        @Test
        public void remove() {
            tree.remove("a");
            assertEquals(0, tree.size());
            assertEquals(-1, tree.get("a"));
            assertKeys(tree);
        }

    }

    private static List<String> pre(String... preset) {
        return Arrays.asList(preset);
    }

    private static List<String> pre(int from, int to) {
        List<String> str = new ArrayList<>();
        for (int i = from; i <= to; i++) {
            str.add(String.format("%02d", i));
        }
        return str;
    }



    public static class PutConsistency {

        private static final int BTREE_T = 2;

        private static void assertPages(Counter ctr, int expected) {
            int blocks = (int)(ctr.getSize() / blockSize(BTREE_T));
            assertEquals("Tree acquired more/less block than expected", expected, blocks - 1);  //1 is for TreeInfo
        }



        private void putAndThenCheck(BTree tree, List<String> preset, String... expectedKeys) {
            for (String key: preset) {
                tree.put(key, key.hashCode());
            }
            //tree.dump(System.out);
            ctr.resetCounters();
            for (String key: expectedKeys) {
                tree.put(key, key.hashCode());
            }
            //tree.dump(System.out);
            List<String> expected = new ArrayList<>( Arrays.asList(expectedKeys));
            expected.addAll(preset);
            List<String> actual = TestTool.iteratorToList(tree);
            assertListsEqual("Tree keys do not match expectations", expected, actual);
            assertEquals("Tree keys match expectations, but tree size is declared incorrectly", expected.size(), tree.size());
            for (String key: expectedKeys) {
                assertTrue("Tree keys match expectations, but key '" + key + "' cannot be found in tree", tree.contains(key));
            }
        }


        private BTree tree;
        private Counter ctr;

        @Before
        public void setup() {
            ctr = new Counter(new MemoryContainer());
            tree = createTree(BTREE_T, ctr);
        }

        @Test
        public void almostFillFirstNode() {
             //     [1]
             // + 2 =>
             //     [1,2]
            putAndThenCheck(tree, pre("1"), "2");
            assertPages(ctr, 1);
        }


        @Test
        public void splitFirstNode() {
            //      [1,2]
            // + 3 =>
            //    [2,  |]
            //    [1] [3]
            putAndThenCheck(tree, pre("1", "2"), "3");
            assertPages(ctr, 3);
        }


        @Test
        public void addToFirstSubnode() {
            //    [2,  |]
            //    [1] [3]
            // + 0 =>
            //    [2,   |]
            //    [0,1] [3]
            putAndThenCheck(tree, pre("1", "2", "3"), "0");
            assertPages(ctr, 3);

        }

        @Test
        public void addToLastSubnode() {
            //    [2,  |]
            //    [1] [3]
            // + 4 =>
            //    [2, |]
            //    [1] [3,4]
            putAndThenCheck(tree, pre("1", "2", "3"), "4");
            assertPages(ctr, 3);

        }

        @Test
        public void overflowLastSubnode() {
            //    [2, |]
            //    [1] [3,4]
            // + 5 =>
            //    [2,  4, |]
            //    [1] [3] [5]
            putAndThenCheck(tree, pre("1", "2", "3", "4"), "5");
            assertPages(ctr, 4);
        }

        @Test
        public void overflowCascade() {
            //    [2,  4, |]
            //    [1] [3] [5]
            // + 6,7 =>
            //    [4,       |]
            //    [2, |]   [6, |]
            //    [1] [3]  [5] [7]
            putAndThenCheck(tree, pre("1", "2", "3", "4", "5"), "6", "7");
            assertPages(ctr, 7);
        }

        @Test
        public void overflowCascade_firstSubnode() {
            //    [20,  40, |]
            //    [10] [30] [50]
            // + 5, 8 =>
            //    [20,     |]
            //    [8, |]   [40,  |]
            //    [5] [10] [30]  [50]
            putAndThenCheck(tree, pre("10", "20", "30", "40", "50"), "05", "08");
            assertPages(ctr, 7);

        }

        @Test
        public void fillTreeLevels() {
            putAndThenCheck(tree, pre("10", "20", "30", "40", "50", "05", "08"), "04", "11", "31", "41");
            assertPages(ctr, 7);
        }

        @After
        public void log() {
            //tree.dump(System.out);
        }


    }

    public static class RemoveConsistency {

        private Counter ctr;
        private BTree tree;
        private final static int BTREE_T = 3;

        @Before
        public void setup() {
            ctr = new Counter(new MemoryContainer());
            tree = createTree(BTREE_T, ctr);
        }

        private void removeAndThenCheck(BTree tree, List<String> preset, String... removedKeys) {
            for (String key: preset) {
                tree.put(key, key.hashCode());
            }
            //System.out.println("### Before delete");
            //tree.dump(System.out);
            ctr.resetCounters();
            List<String> expected = new ArrayList<>( preset);
            for (String key: removedKeys) {
                tree.remove(key);
                expected.remove(key);
            }
            //System.out.println("### After delete");
            //tree.dump(System.out);
            List<String> actual = TestTool.iteratorToList(tree);
            assertListsEqual("Tree keys do not match expectations", expected, actual);
            assertEquals("Tree keys match expectations, but tree size is declared incorrectly", expected.size(), tree.size());
            for (String key: removedKeys) {
                assertTrue("Tree keys match expectations, but removed key '" + key + "' can be found in tree", !tree.contains(key));
            }
            for (String key: expected) {
                assertTrue("Tree keys match expectations, but remaining key '" + key + "' cannot be found in tree", tree.contains(key));
            }
        }

        @Test
        public void singleElement() {
            //    [1]
            // - 1 =>
            //    []
            removeAndThenCheck(tree, pre("1"),"1");
        }

        @Test
        public void leafElement_uniteNodes() {
            //    [3,   |]
            //    [1,2][4,5]
            // - 1 =>
            //    [2,3,4,5]
            removeAndThenCheck(tree, pre("1","2","3","4","5"), "1");
        }

        @Test
        public void leafElement_noShrink() {
            //    [4,     |]
            //    [1,2,3][5,6,7]
            // - 1 =>
            //    [4,     |]
            //    [2,3][5,6,7]
            removeAndThenCheck(tree, pre("1","2","4","5","6","3","7"), "1");
        }

        @Test
        public void leafElement_replaceParent() {
            //  [4,     |]
            //  [2,3]  [5,6,7]
            // - 2 =>
            //  [3,4,5,6,7]
            removeAndThenCheck(tree, pre("1","2","4","5","6","3","7"), "1", "2");
        }

        @Test
        public void parentElement_root() {
            //    [3,   |]
            //    [1,2][4,5]
            // - 3 =>
            //    [1,2,4,5]
            removeAndThenCheck(tree, pre("1","2","3","4","5"), "3");
        }

        @Test
        public void shift_left() {
            //      [10,     |]
            //      [6,7]    [11,12,13,14]
            // - 6 =>
            //      [11,     |]
            //      [7,10]  [12,13,14]
            removeAndThenCheck(tree, pre("6","7","10","11","12","13","14"), "6");
        }

        @Test
        public void shift_right() {
            //      [10,      |]
            //      [1,2,3,4][12,13]
            // - 12 =>
            //      [4,     |]
            //      [1,2,3][10,13]
            removeAndThenCheck(tree, pre("1","2","10","12","13","3","4"), "12");
        }

        @Test
        public void nonRoot_withChildren() {
            //     [9,              18,                  |]
            //     [3,   6,   |]   [12,   15,    |]      [21,   24,    27,    |]
            //     [1,2][4,5][7,8] [10,11][13,14][16,17][19,20][22,23][25,26][28,29,30]
            // - 15 =>
            //

            removeAndThenCheck(tree, pre(1,30), "15");
        }

        @Test
        public void shift_nodeWithChildren() {
            //     [9,              18,                  |]
            //     [3,   6,   |]   [12,   15,    |]      [21,   24,    27,    |]
            //     [1,2][4,5][7,8] [10,11][13,14][16,17][19,20][22,23][25,26][28,29,30]
            // - 15 =>
            //

            removeAndThenCheck(tree, pre(1,30), "21", "24");
        }

        @Test
        public void delete_lot_of() {
            //     [9,              18,                  |]
            //     [3,   6,   |]   [12,   15,    |]      [21,   24,    27,    |]
            //     [1,2][4,5][7,8] [10,11][13,14][16,17][19,20][22,23][25,26][28,29,30]
            removeAndThenCheck(tree, pre(1,30), "12","15","18","24","29","03","01","04","30","16","10","17","11","22","23","25","26","28","07","08","05","06");
        }


    }

}
