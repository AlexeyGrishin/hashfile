package io.github.alexeygrishin.hashfile;

import io.github.alexeygrishin.btree.KeyTruncateMethod;
import io.github.alexeygrishin.hashfile.btreebased.BTreeBasedFactory;
import io.github.alexeygrishin.tool.ByteCounter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static io.github.alexeygrishin.tool.TestTool.*;
import static org.junit.Assert.*;

public class NamedStorageIntegrationTest {


    public static abstract class Base {
        protected abstract int getKeyLen();
        protected abstract KeyTruncateMethod getTruncateMethod();

        protected abstract int getDataLen();
        private NamedStorage storage;



        private final static String TEMP_FILE = "temp1";

        @Before
        public void setup() {
            deleteTempFile();
            storage = new BTreeBasedFactory().create(TEMP_FILE, null, null, getTruncateMethod());
        }


        private void deleteTempFile() {
            File file = new File(TEMP_FILE);
            if (file.exists()) {
                if (!file.delete()){
                    throw new RuntimeException("Cannot delete file " + TEMP_FILE);
                }
            }
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
