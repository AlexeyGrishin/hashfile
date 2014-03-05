package io.github.alexeygrishin.bytestorage;

import java.nio.ByteBuffer;

/**
 * In-memory bytes container, just for testing
 */
public class MemoryContainer implements SynchronizedByteContainer {

    private ByteBuffer bytes;

    public MemoryContainer() {
        bytes = ByteBuffer.allocate(0);
    }

    @Override
    public synchronized void read(long position, ByteBuffer target) {
        bytes.position((int)position);
        target.clear();
        while (target.hasRemaining()) {
            target.put(bytes.get());
        }
    }

    @Override
    public synchronized void write(long position, ByteBuffer target) {
        bytes.position((int)position);
        target.rewind();
        while (target.hasRemaining()) {
            bytes.put(target.get());
        }
    }

    @Override
    public synchronized long append(ByteBuffer target) {
        ByteBuffer newBuffer = ByteBuffer.allocate(bytes.capacity() + target.capacity());
        long pos = bytes.capacity();
        bytes.rewind();
        newBuffer.put(bytes);
        target.rewind();
        newBuffer.put(target);
        bytes = newBuffer;
        return pos;
    }

    @Override
    public synchronized long getSize() {
        return bytes.capacity();
    }

    @Override
    public void close() {
        //nothing
    }

}
