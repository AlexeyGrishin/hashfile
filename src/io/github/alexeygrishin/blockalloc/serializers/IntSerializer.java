package io.github.alexeygrishin.blockalloc.serializers;

import java.nio.ByteBuffer;

class IntSerializer implements Serializer<Integer> {
    @Override
    public void save(ByteBuffer buffer, Integer instance) {
        buffer.putInt(instance != null ? instance : 0);
    }

    @Override
    public Integer load(ByteBuffer buffer) {
        return buffer.getInt();
    }

    @Override
    public int getSize() {
        return Integer.SIZE >> 3;
    }
}
