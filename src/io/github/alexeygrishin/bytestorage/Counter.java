package io.github.alexeygrishin.bytestorage;

import java.io.PrintStream;
import java.nio.ByteBuffer;

/**
 * Counts read/write/append operations, useful for tests or debugging
 */
public class Counter implements SynchronizedByteContainer {

    private SynchronizedByteContainer wrapped;
    private int reads, writes, appends;

    public Counter(SynchronizedByteContainer wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void read(long position, ByteBuffer target) {
        reads++;
        wrapped.read(position, target);
    }

    @Override
    public void write(long position, ByteBuffer target) {
        writes++;
        wrapped.write(position, target);
    }

    @Override
    public long append(ByteBuffer target) {
        appends++;
        return wrapped.append(target);
    }

    @Override
    public long getSize() {
        return wrapped.getSize();
    }

    @Override
    public void close() {
        wrapped.close();
    }

    public void resetCounters() {
        reads = writes = appends = 0;
    }

    public int getReads() {
        return reads;
    }

    public int getWrites() {
        return writes;
    }

    public int getAppends() {
        return appends;
    }

    public void dump(PrintStream out) {
        out.println("reads = " + reads + ", writes = " + writes + ", appends = " + appends);
    }
}
