package io.github.alexeygrishin.bytestorage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Operates with file via nio FileChannel.
 */
public class FileBytesContainer implements SynchronizedByteContainer {

    private final FileChannel channel;

    public FileBytesContainer(FileChannel channel) {
        this.channel = channel;
    }

    @Override
    public void read(long position, ByteBuffer target) {
        try {
            checkRanges(position, target.capacity());
            target.clear();
            channel.read(target, position);
        }
        catch (IOException e) {
            throw new StorageFault(e);
        }
    }

    private void checkRanges(long position, int length) {
        long size = getSize();
        if (position < 0 || position + length > size) {
            throw new IndexOutOfBoundsException(
                    String.format("Attempt to access data outside file contents: file bounds are 0 - %d, required range is %d - %d", size, position, position + length)
            );
        }
    }

    @Override
    public void write(long position, ByteBuffer target) {
        try {
            checkRanges(position, target.capacity());
            target.rewind();
            channel.write(target, position);
        }
        catch (IOException e) {
            throw new StorageFault(e);
        }
    }

    @Override
    public long append(ByteBuffer target) {
        try {
            synchronized (channel) {
                long position = channel.size();
                target.rewind();
                channel.write(target, position);
                return position;
            }
        }
        catch (IOException e) {
            throw new StorageFault(e);
        }
    }

    @Override
    public long getSize() {
        try {
            return channel.size();
        }
        catch (IOException e) {
            throw new StorageFault(e);
        }
    }

    @Override
    public void close() {
        try {
            channel.close();
        } catch (IOException e) {
            throw new StorageFault(e);
        }
    }
}
