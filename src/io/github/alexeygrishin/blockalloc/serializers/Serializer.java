package io.github.alexeygrishin.blockalloc.serializers;

import java.nio.ByteBuffer;

/**
 * Allows to store object to byte buffers and read it back.
 * Subclassess provides loading/saving for different types.
 *
 * The most important implementation is {@link io.github.alexeygrishin.blockalloc.serializers.StructSerializer}.
 * See also {@link io.github.alexeygrishin.blockalloc.serializers.Serializers}
 */
public interface Serializer<T> {
    /**
     * Saves specified instance to buffer.
     * It is expected that exact {@link #getSize()} bytes will be stored to buffer. If there is no enough data
     * in instance then some zero bytes shall be added for padding.
     * @param buffer buffer to write into. Implementations shall NOT touch position directly.
     * @param instance object to write.
     * @throws SerializationException on any problem with serialization - type mismatch and so on.
     */
    public void save(ByteBuffer buffer, T instance);

    /**
     * Loads object from buffer.
     * It is expected that exact {@link #getSize()} bytes will be loaded from buffer.
     * @param buffer buffer to read from. Implementations shall NOT touch position directly or modify the buffer.
     * @return instance read from buffer
     * @throws SerializationException on any problem with serialization - type mismatch and so on.
     */
    public T load(ByteBuffer buffer);

    /**
     *
     * @return size in bytes enough to serialize object this serializer corresponds to.
     */
    public int getSize();
}
