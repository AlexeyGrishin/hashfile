package io.github.alexeygrishin.hashfile;

import io.github.alexeygrishin.btree.KeyTruncateMethod;
import io.github.alexeygrishin.hashfile.btreebased.BTreeBasedFactory;
import io.github.alexeygrishin.tool.ByteCounter;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static io.github.alexeygrishin.tool.TestTool.*;
import static org.junit.Assert.*;

public class NamedStorageIntegrationTest {

    private final static String TEMP_FILE = "temp1";


    private static  void deleteTempFile() {
        File file = new File(TEMP_FILE);
        if (file.exists()) {
            if (!file.delete()){
                throw new RuntimeException("Cannot delete file " + TEMP_FILE);
            }
        }
    }

    public static class Put4M {
        public static final int HOW_MANY = 4000000;
        public static final int TIMEOUT_4M = 1000 * 60 * 3;
        protected NamedStorage storage;


        @Before
        public void setup() {
            deleteTempFile();
            storage = new BTreeBasedFactory().create(TEMP_FILE, 1, 64, KeyTruncateMethod.LEADING);
        }

        @Test(timeout = TIMEOUT_4M)
        @Ignore
        //TODO: replace with performance test
        public void add_4M_andIterate() {
            long now = System.nanoTime();
            for (long i = 0; i < HOW_MANY; i++) {
                storage.saveFrom(randomString(10),generateData(1));
                if (i % 100000 == 0) {
                    long now2 = System.nanoTime();
                    long diff = now2 - now;
                    System.out.println((i*100 / HOW_MANY) + "%\t" + diff);
                    now = now2;
                }
            }
            int counter = 0;
            for (String ctr: storage) {
                counter++;
            }
            assertEquals(HOW_MANY, counter);
        }

        @After
        public void teardown() {
            if (storage != null) storage.close();
            storage = null;
            deleteTempFile();
        }

    }

    public static abstract class Base {
        protected abstract int getKeyLen();
        protected abstract KeyTruncateMethod getTruncateMethod();

        protected abstract int getDataLen();
        protected NamedStorage storage;



        @Before
        public void setup() {
            deleteTempFile();
            storage = new BTreeBasedFactory().create(TEMP_FILE, null, null, getTruncateMethod());
        }


        @After
        public void teardown() {
            if (storage != null) storage.close();
            storage = null;
            deleteTempFile();
        }

        @Test
        public void putAndContain() {
            String key = randomString(getKeyLen());
            storage.saveFrom(key, generateData(getDataLen()));
            assertTrue(storage.contains(key));
        }

        @Test
        public void putAndGet() {
            String key = randomString(getKeyLen());
            storage.saveFrom(key, generateData(getDataLen()));
            ByteCounter ctr = new ByteCounter();
            storage.getInto(key, ctr);
            assertEquals(getDataLen(), ctr.getCounted());
        }

        @Test
        public void replaceLargerAndGet() {
            String key = randomString(getKeyLen());
            storage.saveFrom(key, generateData(getDataLen()));

            storage.saveFrom(key, generateData(getDataLen() + 15));
            ByteCounter ctr = new ByteCounter();

            storage.getInto(key, ctr);
            assertEquals(getDataLen() + 15, ctr.getCounted());
        }

        @Test
        public void replaceSmallerAndGet() {
            String key = randomString(getKeyLen());
            storage.saveFrom(key, generateData(getDataLen()));

            int expectedLen = getDataLen() / 2;
            storage.saveFrom(key, generateData(expectedLen));

            ByteCounter ctr = new ByteCounter();
            storage.getInto(key, ctr);
            assertEquals(expectedLen, ctr.getCounted());
        }

        @Test
        public void putAndIterate() {
            String key = randomString(getKeyLen());
            storage.saveFrom(key, generateData(getDataLen()));
            List<String> keys = iteratorToList(storage);
            assertEquals(Arrays.asList(key), keys);
        }

        @Test
        public void putAndDelete() {
            String key = randomString(getKeyLen());
            storage.saveFrom(key, generateData(getDataLen()));
            storage.delete(key);
            assertFalse(storage.contains(key));

        }

        @Test
        public void save_load() {
            storage.saveFrom("01", generateData(getDataLen()));
            storage.close();
            storage = new BTreeBasedFactory().load(TEMP_FILE);
            assertTrue(storage.contains("01"));
        }


    }

    public static class ShortKeySmallData extends Base {
        @Override
        protected int getKeyLen() {
            return 30;
        }

        @Override
        protected KeyTruncateMethod getTruncateMethod() {
            return KeyTruncateMethod.LEADING;
        }

        @Override
        protected int getDataLen() {
            return 1024;
        }
    }

    public static class LongKeyLeading extends Base {
        @Override
        protected int getKeyLen() {
            return 3000;
        }

        @Override
        protected KeyTruncateMethod getTruncateMethod() {
            return KeyTruncateMethod.LEADING;
        }

        @Override
        protected int getDataLen() {
            return 1024;
        }
    }

    public static class LongKeyTrailing extends Base {
        @Override
        protected int getKeyLen() {
            return 3000;
        }

        @Override
        protected KeyTruncateMethod getTruncateMethod() {
            return KeyTruncateMethod.TRAILING;
        }

        @Override
        protected int getDataLen() {
            return 1024;
        }
    }

    public static class LongKeyAndData extends Base {
        @Override
        protected int getKeyLen() {
            return 10*1000*1000;
        }

        @Override
        protected KeyTruncateMethod getTruncateMethod() {
            return KeyTruncateMethod.LEADING;
        }

        @Override
        protected int getDataLen() {
            return 10*1000*1000;
        }
    }

}
