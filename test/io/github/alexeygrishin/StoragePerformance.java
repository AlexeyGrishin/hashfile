package io.github.alexeygrishin;

import io.github.alexeygrishin.blockalloc.BlockAllocator;
import io.github.alexeygrishin.blockalloc.Cache;
import io.github.alexeygrishin.hashfile.NamedStorage;
import io.github.alexeygrishin.hashfile.btreebased.BTreeBasedFactory;
import io.github.alexeygrishin.hashfile.btreebased.BTreeBasedStorage;
import io.github.alexeygrishin.tool.ByteCounter;
import io.github.alexeygrishin.tool.TestTool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class StoragePerformance {

    public static void main(String args[]) throws FileNotFoundException, InterruptedException {
        StoragePerformance performance = new StoragePerformance();
        performance.doTest(new PrintStream("perf_2.csv"));
    }

    private void doTest(PrintStream out) throws InterruptedException {
        out.println(",Reads/s,Writes/s,Read avg msec,Write avg msec");
        for (int i = 1; i <= 20; i++) {
            doTestStorage(i, 1, 1000, 1000, 100, out);
        }
        for (int i = 1; i <= 20; i++) {
            doTestStorage(1, i, 1000, 1000, 100, out);
        }
        for (int i = 1; i <= 20; i++) {
            doTestStorage(i, i, 1000, 1000, 100, out);
        }
    }

    private void doTestStorage(int readers, final int writers, int keyLen, final int dataLen, final int randomData, PrintStream out) throws InterruptedException {
        File temp = new File("_temp1");
        if (temp.exists()) {
            temp.delete();
        }
        try (NamedStorage storage = new BTreeBasedFactory().create("_temp1")) {
            doReadWrites(storage, readers, writers, keyLen, dataLen, randomData, out);
        }
        if (!temp.delete()) {
            throw new RuntimeException("Cannot close storage");
        }

    }

    private void doReadWrites(final NamedStorage storage, final int readers, final int writers, int keyLen, final int dataLen, int randomData, PrintStream out) throws InterruptedException {
        String hdr = "r=" + readers + " w=" + writers + "[" + keyLen + "/" + dataLen + "/" + randomData + "],";
        out.print(hdr);
        System.out.println(hdr);
        final int opsCount = 10000;
        final int readOps = opsCount / readers;
        final int writeOps = opsCount / writers;
        final List<String> keys = TestTool.randomStrings(randomData, keyLen);

        ExecutorService readService = Executors.newFixedThreadPool(readers);
        ExecutorService writeService = Executors.newFixedThreadPool(writers);
        final long now = TestTool.now();
        final AtomicLong readSum = new AtomicLong(0);
        final AtomicLong writeSum = new AtomicLong(0);
        final AtomicLong readEnded = new AtomicLong(0);
        final AtomicLong writeEnded = new AtomicLong(0);
        final AtomicLong written = new AtomicLong(0);
        final AtomicLong read = new AtomicLong(0);
        for (int i = 0; i < writers; i++) {
            writeService.submit(new Runnable() {

                @Override
                public void run() {
                    for (int i = 0; i < writeOps; i++) {
                        try {
                            //System.out.println("write " + i);
                            String key = keys.get((int) (Math.random() * keys.size()));
                            long rs = TestTool.now();
                            storage.saveFrom(key, TestTool.generateData(dataLen));
                            writeSum.addAndGet(TestTool.delta(rs));
                            written.incrementAndGet();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (written.get() == writeOps * writers) {
                        writeEnded.set(TestTool.delta(now));
                    }
                }
            });
        }
        for (int i = 0; i < readers; i++) {
            readService.submit(new Runnable() {

                @Override
                public void run() {
                    for (int i = 0; i < readOps; i++) {
                        try {
                            //System.out.println("Read " + i);
                            String key = keys.get((int) (Math.random() * keys.size()));
                            long rs = TestTool.now();
                            storage.getInto(key, new ByteCounter());
                            readSum.addAndGet(TestTool.delta(rs));
                            read.incrementAndGet();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (read.get() >= readOps * readers) {
                        readEnded.set(TestTool.delta(now));
                    }
                }
            });
        }
        readService.shutdown();
        writeService.shutdown();
        readService.awaitTermination(10, TimeUnit.MINUTES);
        writeService.awaitTermination(10, TimeUnit.MINUTES);
        System.out.println(String.format("  reader ops = %d, writer ops = %d, total read time = %d msec, total write time = %d msec, sum of reads = %d msec, sum of writes = %d msec",
                readOps, writeOps,  readEnded.get() / 1000000 , writeEnded.get() / 1000000, readSum.get() / 1000000, writeSum.get()  / 1000000
                ));
        double readsPerSec = (double)readers*opsCount / readEnded.get() * 1000000000;
        double writesPerSec = (double)writers*opsCount / writeEnded.get() * 1000000000;
        double readAverageMsec = (double)readSum.get() / readers / opsCount / 1000000;
        double writeAverageMsec = (double)writeSum.get() / writers / opsCount / 1000000;

        out.println(String.format("%.2f,%.2f,%.2f,%.2f", readsPerSec, writesPerSec, readAverageMsec, writeAverageMsec));

    }




}
