package io.github.alexeygrishin;

import io.github.alexeygrishin.blockalloc.BlockAllocator;
import io.github.alexeygrishin.blockalloc.Cache;
import io.github.alexeygrishin.btree.BTree;
import io.github.alexeygrishin.bytestorage.FileBytesContainer;
import io.github.alexeygrishin.bytestorage.Counter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.util.*;
import static io.github.alexeygrishin.tool.TestTool.*;

public class TreeMapRWPerformance {


    public static void main(String args[]) throws FileNotFoundException, InterruptedException {
        TreeMapRWPerformance perf = new TreeMapRWPerformance();
        PrintStream file = new PrintStream("btree_temp.csv");
        perf.doTest2(file);
        file.close();
    }

    private Cache storage;

    private int[] blockSizes = {64*1024, 128*1024, 256*1024, 512*1024, 1024*1024};
    private int[] cacheSizes = {64*1024*1024,/*, 128*1024*1024,*/ 256*1024*1024};

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

        for (int blockSize: (blockSizes)) {
            for (int cacheSize: (cacheSizes)) {
                System.out.println("test for " + blockSize + "x" + cacheSize);
                doTestTree(out, blockSize, cacheSize, 1000000, 40000, true);
            }
        }
    }

    private void doTestTree(PrintStream out, int blockSize, int cacheLimit, int maxCount, int step, boolean noreset) throws FileNotFoundException, InterruptedException {
        File temp1 = new File("temp1");
        if (temp1.exists() && !temp1.delete()) {
            throw new RuntimeException("Cannot delete temp file");
        }
        Counter ctr = new Counter(new FileBytesContainer(new RandomAccessFile("temp1", "rw").getChannel()));
        BlockAllocator allocator = new BlockAllocator(ctr, blockSize);
        storage = new Cache(allocator, cacheLimit);
        out.println("Tree block = " + blockSize + " cache = " + cacheLimit);
        out.println("N, Put time, Put reads, Put writes, Get time, Get reads, Get writes");
        int putsCount = step, readsCount = step;
        try (BTree tree = new BTree(cacheLimit == 0 ? allocator : storage)) {
            List<String> lastStrings = new ArrayList<>(1000);
            int portions = maxCount / step;
            for (int portion = 0; portion < portions; portion++) {
                String generatedKey = randomString(20), suffix = randomString(3);
                lastStrings.clear();
                long now = 0;
                for (int i = 0; i < step; i++) {
                    String key = generatedKey + i + suffix;
                    lastStrings.add(key);
                    tree.put(key, i);
                }
                if (!noreset)
                    storage.reset();
                long putTime = delta(now), putReads = ctr.getReads(), putWrites = ctr.getWrites() + ctr.getAppends();
                ctr.resetCounters();
                now = now();
                for (int i = 0; i < readsCount; i++) {
                    tree.get(lastStrings.get((int) (Math.random()*lastStrings.size())));
                }
                if (!noreset)
                    storage.reset();
                long readTime = delta(now), getReads = ctr.getReads(), getWrites = ctr.getWrites();
                out.println(portion * step + "," + (putTime / putsCount) + "," + (putReads) + "," + (putWrites) +
                        "," + (readTime / readsCount) + "," + (getReads) + "," + (getWrites)
                );

            }
        }
        if (noreset) {
            out.println("Close,," + ctr.getReads() + "," + ctr.getWrites());
        }

        out.println();
        System.gc();
        Thread.sleep(1000);
    }

}
