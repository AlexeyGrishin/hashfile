package io.github.alexeygrishin.blockalloc.serializers;

import java.nio.ByteBuffer;

class ByteArraySerializer implements Serializer<byte[]> {

    private final int maxSize;

    public ByteArraySerializer(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public void save(ByteBuffer buffer, byte[] instance) {
        if (instance.length != maxSize) {
            throw new SerializationException("Provided array's length does not match initially defined for this field");
        }
        buffer.put(instance);
    }

    @Override
    public byte[] load(ByteBuffer buffer) {
        byte[] bytes = new byte[maxSize];
        buffer.get(bytes);
        return bytes;
    }

    @Override
    public int getSize() {
        return maxSize;
    }
}
