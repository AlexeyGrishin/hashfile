package io.github.alexeygrishin.blockalloc.serializers;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;

class ArraySerializer implements Serializer<Object[]> {

    private final Class<?> innerClass;
    private final int size;
    private final int count;
    private final Serializer innerSerializer;

    public ArraySerializer(Class<?> type, int maxSize) {
        this.innerClass = type.getComponentType();
        if (innerClass.isPrimitive()) {
            throw new SerializationException("Arrays of primitive types (except byte) are not supported");
        }
        innerSerializer = Serializers.INSTANCE.get(innerClass);
        int structSize = innerSerializer.getSize();
        this.count = maxSize / structSize;
        if (this.count == 0) {
            throw new SerializationException("Field  is array of " + innerClass + " which size is " + structSize + ", but max size is " + maxSize);
        }
        this.size = this.count * structSize;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void save(ByteBuffer buffer, Object[] instance) {
        if (instance.length != count) {
            throw new SerializationException("Provided array's length does not match initially defined for this field");
        }
        for (Object o: instance) {
            innerSerializer.save(buffer, o);
        }
    }

    @Override
    public Object[] load(ByteBuffer buffer) {
        Object[] result = (Object[]) Array.newInstance(innerClass, count);
        for (int i = 0; i < count; i++) {
            result[i] = innerSerializer.load(buffer);
        }
        return result;
    }

    @Override
    public int getSize() {
        return size;
    }
}
