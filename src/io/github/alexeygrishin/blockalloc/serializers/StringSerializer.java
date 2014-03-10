package io.github.alexeygrishin.blockalloc.serializers;

import java.nio.ByteBuffer;

public class StringSerializer implements Serializer<String> {
    private int maxSize;
    private int maxLength;

    public StringSerializer(int maxSize) {
        this.maxSize = maxSize;
        this.maxLength = maxSize / (Character.SIZE / Byte.SIZE);
    }

    @Override
    public void save(ByteBuffer buffer, String instance) {
        byte[] bf = new byte[maxSize];
        String toSave = instance.substring(0, Math.min(instance.length(), maxLength));
        System.arraycopy(toSave.getBytes(), 0, bf, 0, toSave.length());
        buffer.put(bf);
    }

    @Override
    public String load(ByteBuffer buffer) {
        byte[] bf = new byte[maxSize];
        buffer.get(bf);
        int firstNil = -1;
        for (int i = 0; i < bf.length; i++) {
            if (bf[i] == 0) {
                firstNil = i;
                break;
            }
        }
        return firstNil == -1 ? new String(bf) : new String(bf, 0, firstNil);
    }

    @Override
    public int getSize() {
        return maxSize;
    }
}
