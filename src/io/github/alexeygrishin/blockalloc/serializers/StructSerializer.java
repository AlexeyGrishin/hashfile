package io.github.alexeygrishin.blockalloc.serializers;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

//TODO: tests on edge cases. Probably more examples on serialization
class StructSerializer<T> implements Serializer<T> {

    private Class<T> kls;
    private List<FieldInfo> fields = new ArrayList<>(16);
    private int size;
    private int pad = 0;

    class FieldInfo {
        FieldInfo(Field field, Serializer serializer) {
            this.field = field;
            this.serializer = serializer;
        }

        Field field;
        Serializer serializer;
    }

    public StructSerializer(Class<T> kls) {
        this(kls, getClassSize(kls));
    }

    public StructSerializer(Class<T> kls, Integer requiredSize) {
        this.kls = kls;
        this.size = 0;
        Field unlimitedArrayField = null;

        for (Field field: kls.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) continue;
            if (field.getType().isArray()) {
                if (unlimitedArrayField != null) {
                    throw new IllegalStateException("Class " + kls + " has two array fields. Only one field may be array");
                }
                unlimitedArrayField = field;
                continue;
            }
            Serializer serializer = getSerializerFor(field);
            fields.add(new FieldInfo(field, serializer));
            size += serializer.getSize();
        }
        if (requiredSize != null) {
            if (requiredSize < size) {
                throw new IllegalStateException("Class " + kls + " has real size of " + size + "bytes, but @Limited annotation declares only " + requiredSize);
            }
            if (unlimitedArrayField != null) {
                Serializer serializer = Serializers.INSTANCE.get(unlimitedArrayField.getType(), requiredSize - size);
                fields.add(new FieldInfo(unlimitedArrayField, serializer));
                size += serializer.getSize();
            }
            pad = requiredSize - size;
            size = requiredSize;
        }
        else if (unlimitedArrayField != null) {
            throw new IllegalStateException("Class " + kls + " has array field, but does not have @Limited annotation, so serializer cannot define its size");
        }
        if (fields.isEmpty()) {
            throw new IllegalArgumentException("Class " + kls + " does not have any field to serialize. Probably fields are not defined as public");
        }
    }

    private static Integer getClassSize(Class<?> kls) {
        return kls.isAnnotationPresent(Limited.class) ? kls.getAnnotation(Limited.class).size() : null;
    }

    protected Serializer getSerializerFor(Field field) {
        return Serializers.INSTANCE.get(field);
    }

    @Override
    public final void save(ByteBuffer buffer, T instance) {
        try {
            for (FieldInfo entry: fields) {
                entry.serializer.save(buffer, entry.field.get(instance));
            }
            skipPad(buffer);
        }
        catch (IllegalAccessException  e) {
            throw new SerializationException(e);
        }
    }

    private void skipPad(ByteBuffer buffer) {
        if (pad > 0) {
            buffer.position(buffer.position() + pad);
        }
    }

    @Override
    public final T load(ByteBuffer buffer) {
        try {
            T instance = kls.newInstance();
            for (FieldInfo entry: fields) {
                entry.field.set(instance, entry.serializer.load(buffer));
            }
            skipPad(buffer);
            return instance;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public final int getSize() {
        return size;
    }
}
