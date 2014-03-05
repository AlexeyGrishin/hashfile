package io.github.alexeygrishin.blockalloc.serializers;

import java.nio.ByteBuffer;

class ByteSerializer implements Serializer<Byte> {
    @Override
    public void save(ByteBuffer buffer, Byte instance) {
        buffer.put(instance != null ? instance : 0);
    }

    @Override
    public Byte load(ByteBuffer buffer) {
        return buffer.get();
    }

    @Override
    public int getSize() {
        return Byte.SIZE >> 3;
    }
}
