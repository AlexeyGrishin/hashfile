package io.github.alexeygrishin.blockalloc.serializers;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Serializers factory which caches once created serializers. Allows to produce serializers for any classes that meet following requirements:
 * 1. Are public (and static for inner classes), i.e. Class.newInstance() shall work
 * 2. Have constructor without params
 * 3. Have at least one public field - only public non-static non-transient fields will be serialized
 * 4. For String fields there shall be {@link io.github.alexeygrishin.blockalloc.serializers.Limited} anotation with max size
 * 5. There shall be zero or one array of bytes or other classes
 * Also such class may have {@link io.github.alexeygrishin.blockalloc.serializers.Limited} annotation to explicitly define its class.
 *
 * Example:
 * <code>
 *     @Limited(length = 100);
 *     class Struct1 {
 *         public int value1;
 *         @Limited(length = 20);
 *         public String name;
 *     }
 *     ...
 *     Struct s = Serializers.INSTANCE.get(Struct1.class).load(byteBuffer);
 * </code>
 *
 * If {@link #get} is called for class which implements {@link Serializer} then instance of this class will be used as serializer.
 * This way custom serializers may be created if default one does not fit the needs.
 *
 * Why this could be used instead of DataInputStream/DataOutputStream
 * 1. it works with ByteBuffer instead of streams
 * 2. it does not require writing serialization/deserialization code
 */
public class Serializers {

    private Map<Class, Serializer> serializers = new HashMap<>();
    private Map<SizedClassKey, Serializer> sizedSerializers = new HashMap<>();
    {
        serializers.put(Integer.TYPE, new IntSerializer());
        serializers.put(Integer.class, new IntSerializer());
        serializers.put(Long.TYPE, new LongSerializer());
        serializers.put(Long.class, new LongSerializer());
        serializers.put(Byte.TYPE, new ByteSerializer());
        serializers.put(Byte.class, new ByteSerializer());
    }

    /**
     * Gets serializer for provided class.
     * @throws SerializationException if serializer cannot be created
     */
    @SuppressWarnings("unchecked")
    public <T> Serializer<T> get(Class<T> kls) {
        Serializer ser = serializers.get(kls);
        if (ser == null) {
            ser = getSerializerFromClass(kls);
        }
        if (ser == null) {
            if (kls.isArray()) {
                throw new SerializationException("Non-limited arrays are not supported. Call to get(Class, int)");
            }
            ser = new StructSerializer(kls);
            serializers.put(kls, ser);
        }
        return ser;
    }


    /**
     * Same as {@link #get(Class)} but class length is specified in argument.
     * This method creates separate serializer for each unique maxSize.
     *
     * If class itself has less data than maxSize then the remaining will be filled with zeros on write and skipped on read.
     */
    @SuppressWarnings("unchecked")
    public <T> Serializer<T> get(Class<T> kls, int maxSize) {
        SizedClassKey key = new SizedClassKey(kls, maxSize);
        Serializer ser = sizedSerializers.get(key);
        if (ser == null) {
            ser = getSerializerFromClass(kls);
        }
        if (ser == null) {
            if (kls.isArray()) {
                ser = kls == byte[].class ? new ByteArraySerializer(maxSize) : new ArraySerializer(kls, maxSize);
            }
            else {
                ser = new StructSerializer(kls, maxSize);
            }
            sizedSerializers.put(key, ser);
        }
        return ser;
    }

    public Serializer get(Field field) {
        Class fieldClass = field.getType();
        if (fieldClass == String.class) {
            Limited limited = field.getAnnotation(Limited.class);
            if (limited == null) {
                throw new IllegalArgumentException("String field shall have @Limited annotation");
            }
            return new StringSerializer(limited.size());
        }
        else {
            return get(fieldClass);
        }
    }

    public int getSize(Object o) {
        return get(o.getClass()).getSize();
    }

    public int getSize(Class<?> kls) {
        return get(kls).getSize();
    }

    public static final Serializers INSTANCE = new Serializers();

    private Serializer getSerializerFromClass(Class<?> kls) {
        if (Serializer.class.isAssignableFrom(kls)) {
            try {
                return (Serializer) kls.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new SerializationException("Cannot instantiate serializer " + kls, e);
            }
        }
        return null;
    }

    private class SizedClassKey {
        public final Class klass;
        public final int size;

        private SizedClassKey(Class klass, int size) {
            this.klass = klass;
            this.size = size;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SizedClassKey that = (SizedClassKey) o;

            if (size != that.size) return false;
            if (!klass.equals(that.klass)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = klass.hashCode();
            result = 31 * result + size;
            return result;
        }
    }


}
