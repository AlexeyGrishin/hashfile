package io.github.alexeygrishin.bytestorage;

import java.nio.ByteBuffer;

/**
 * Provides thread-safe random access to the "array" of bytes.
 */
public interface SynchronizedByteContainer {

    /**
     * Reads bytes into the buffer from specified position.
     * @param position shall be >=0 and < getSize()
     * @param target buffer to be filled. Its position will be reset before writing
     * @throws java.lang.IndexOutOfBoundsException if trying to read outside container
     * @throws StorageFault on any IO problem
     */
    void read(long position, ByteBuffer target);

    /**
     * Writes bytes into the buffer from specified position.
     * It is guaranteed that next {@link #read(long, java.nio.ByteBuffer)} calls will return updated data.
     * But please note that data may be buffered in memory before writing to IO device/file/whatever.
     * @param position shall be >=0 and < getSize()
     * @param target buffer to be read. Its position will be reset before reading
     * @throws java.lang.IndexOutOfBoundsException if trying to read outside container
     * @throws StorageFault on any IO problem
     */
    void write(long position, ByteBuffer target);


    /**
     * Appends data to the end of container
     * @param target buffer to be read. Its position will be reset before reading
     * @return start position of the written data
     * @throws StorageFault on any IO problem
     */
    long append(ByteBuffer target);

    /**
     *
     * @return container's size, in bytes
     * @throws StorageFault on any IO problem
     */
    long getSize();

    /**
     * Closes corresponding IO device or file (if any) and guarantely flushes all data.
     */
    void close();
}
