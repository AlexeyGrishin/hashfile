package io.github.alexeygrishin;

import io.github.alexeygrishin.blockalloc.BlockAllocator;
import io.github.alexeygrishin.blockalloc.Cache;
import io.github.alexeygrishin.btree.BTree;
import io.github.alexeygrishin.btree.KeyTruncateMethod;
import io.github.alexeygrishin.bytestorage.Counter;
import io.github.alexeygrishin.bytestorage.FileBytesContainer;
import io.github.alexeygrishin.bytestorage.SynchronizedByteContainer;
import io.github.alexeygrishin.hashfile.NamedStorage;
import io.github.alexeygrishin.hashfile.btreebased.BTreeBasedFactory;
import io.github.alexeygrishin.tool.ByteCounter;
import io.github.alexeygrishin.tool.TestTool;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static io.github.alexeygrishin.tool.TestTool.*;

public class StorageRWPerformance {


    public static void main(String args[]) throws FileNotFoundException, InterruptedException {
        StorageRWPerformance perf = new StorageRWPerformance();
        PrintStream file = new PrintStream("storage_test1.csv");
        perf.doTest2(file);
        file.close();
    }

    private int[] dataBlockSizesKb = {1, 4, 128, 1024};
    private int[] dataSizesKb = {1, 100, 10000};
    private int[] keyLengths = {80, 800, 2048};
    private int[] cacheSizesMb = {32, 64};

    private int[] reverse(int[] array) {
        int[] newar = new int[array.length];
        System.arraycopy(array, 0, newar, 0, array.length);
        for (int i = 0; i < array.length / 2; i++) {
            newar[i] = array[array.length - 1 - i];
            newar[array.length - 1 - i] = array[i];
        }
        return newar;
    }

    private void doTest2(PrintStream out) throws FileNotFoundException, InterruptedException {

        StringBuilder summary = new StringBuilder();
        summary.append("Key len, Data KB, block KB, cache MB, Case, Put time, Get time, Put reads, Put writes, Get reads\n");
        summary.append(doStorageTest(out, 4, 64, 1, 80, 1000000, 40000)).append("\n");

        summary.append(doStorageTest(out, 1, 32, 1, 80, 1000000, 40000)).append("\n");
        summary.append(doStorageTest(out, 1, 32, 1, 2048, 1000000, 40000)).append("\n");
        summary.append(doStorageTest(out, 10, 32, 100, 80, 10000, 400)).append("\n");
        summary.append(doStorageTest(out, 128, 32, 100, 80, 10000, 400)).append("\n");
        summary.append(doStorageTest(out, 128, 64, 100, 80, 10000, 400)).append("\n");
        summary.append(doStorageTest(out, 128, 32, 100, 2048, 10000, 400)).append("\n");
        summary.append(doStorageTest(out, 1024, 32, 100, 2048, 10000, 400)).append("\n");
        summary.append(doStorageTest(out, 1024, 32, 100, 80, 10000, 400)).append("\n");
        summary.append(doStorageTest(out, 1024, 32, 100, 2048, 10000, 400)).append("\n");
        summary.append(doStorageTest(out, 1024, 32, 10000, 80, 100, 4)).append("\n");
        summary.append(doStorageTest(out, 1024, 64, 10000, 80, 100, 4)).append("\n");
        summary.append(doStorageTest(out, 1024, 64, 10000, 2048, 100, 4)).append("\n");
        out.println();
        out.println(summary.toString());

    }

    private Counter ctr;

    private String doStorageTest(PrintStream out, int blockSize, int cacheLimit, int dataSizeKB, int keySize, int maxCount, int step) throws FileNotFoundException, InterruptedException {
        File temp1 = new File("temp1");
        if (temp1.exists() && !temp1.delete()) {
            throw new RuntimeException("Cannot delete temp file");
        }


        NamedStorage storage = new BTreeBasedFactory() {
            @Override
            protected SynchronizedByteContainer createBytesContainer(File file) throws IOException {
                return ctr = new Counter(new FileBytesContainer(new RandomAccessFile("temp1", "rw").getChannel()));
            }
        }.create("temp1", blockSize, cacheLimit, KeyTruncateMethod.LEADING);

        out.println("datablock = " + blockSize + " cache = " + cacheLimit + " dataSizeKB = " + dataSizeKB + " keySize = " + keySize);
        long now1 = now();
        String header = String.format("%d,%d,%d,%d,[%d]=%d KB (b: %d KB c:%d MB),", keySize, dataSizeKB, blockSize, cacheLimit, keySize, dataSizeKB, blockSize, cacheLimit);
        System.out.print(header);
        String lastLine = "";
        out.println("N, Put time, Get time, Put reads, Put writes, Get reads, Get writes");
        int putsCount = step, readsCount = step;
        try {
            List<String> lastStrings = new ArrayList<>(1000);
            int portions = maxCount / step;
            for (int portion = 0; portion < portions; portion++) {
                String generatedKey = randomString(keySize);
                lastStrings.clear();
                long now = now();
                for (int i = 0; i < step; i++) {
                    String key = i + generatedKey + i;
                    lastStrings.add(key);
                    storage.saveFrom(key, TestTool.generateData(dataSizeKB*1024));
                }

                long putTime = delta(now), putReads = ctr.getReads(), putWrites = ctr.getWrites() + ctr.getAppends();
                ctr.resetCounters();
                now = now();
                for (int i = 0; i < readsCount; i++) {
                    storage.getInto(lastStrings.get((int) (Math.random() * lastStrings.size())), TestTool.ignoreData());
                }

                long readTime = delta(now), getReads = ctr.getReads(), getWrites = ctr.getWrites();
                out.println(portion * step + "," + (putTime / putsCount) + "," + (readTime / readsCount) + "," + (putReads) + "," + (putWrites) +
                         "," + (getReads) + "," + (getWrites)
                );
                lastLine = (putTime / putsCount) + "," + (readTime / readsCount) + "," + (putReads) + "," + (putWrites) +
                        "," + (getReads) + "," + (getWrites);

            }
        }
        finally {
            storage.close();
        }
        out.println("Close,,," + ctr.getReads() + "," + ctr.getWrites());

        out.println();
        System.out.println( (delta(now1) / 1000 / 1000));
        System.gc();
        Thread.sleep(1000);
        return header + lastLine;
    }

}
