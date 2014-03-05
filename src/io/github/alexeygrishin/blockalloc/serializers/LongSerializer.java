package io.github.alexeygrishin.blockalloc.serializers;

import java.nio.ByteBuffer;

class LongSerializer implements Serializer<Long> {
    @Override
    public void save(ByteBuffer buffer, Long instance) {
        buffer.putLong(instance != null ? instance : 0);
    }

    @Override
    public Long load(ByteBuffer buffer) {
        return buffer.getLong();
    }

    @Override
    public int getSize() {
        return Long.SIZE >> 3;
    }
}
