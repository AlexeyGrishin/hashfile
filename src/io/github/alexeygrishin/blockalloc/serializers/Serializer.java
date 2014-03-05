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
    public void save(ByteBuffer buffer, T instance);
    public T load(ByteBuffer buffer);
    public int getSize();
}
