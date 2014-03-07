package io.github.alexeygrishin.serialization;

import io.github.alexeygrishin.blockalloc.serializers.Limited;
import io.github.alexeygrishin.blockalloc.serializers.SerializationException;
import io.github.alexeygrishin.blockalloc.serializers.Serializer;
import io.github.alexeygrishin.blockalloc.serializers.Serializers;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class SerializersTest {

    private ByteBuffer buffer;

    @Before
    public void setup() {
        buffer = ByteBuffer.allocate(200);
    }

    @Test
    public void structWithArray() {
        Serializer<StructWithArray> serializer = Serializers.INSTANCE.get(StructWithArray.class);
        StructWithArray instance = serializer.load(buffer);
        assertEquals(25, instance.array.length);
    }

    @Test
    public void structWithArray_save_load() {
        Serializer<StructWithArray> serializer = Serializers.INSTANCE.get(StructWithArray.class);
        StructWithArray instance = new StructWithArray();
        instance.array = new Integer[25];
        instance.array[11] = 5;
        serializer.save(buffer, instance);
        buffer.rewind();
        instance = serializer.load(buffer);
        assertEquals(5, (int)instance.array[11]);
    }
    
    @Test
    public void structWithArrayOfStructs() {
        Serializer<StructWithStructArray> serializer = Serializers.INSTANCE.get(StructWithStructArray.class);
        StructWithStructArray s = serializer.load(buffer);
        assertEquals(2, s.structs.length);
        s.structs[0] = new InnerStruct();
        s.structs[0].name = "test";
        buffer.clear();
        serializer.save(buffer, s);
        buffer.rewind();
        s = serializer.load(buffer);
        assertEquals("test", s.structs[0].name);
    }
    
    @Test
    //TODO: no primitives
    public void structWithArrayOfPrimitives() {
        Serializer<StructWithPrimitiveArray> serializer = Serializers.INSTANCE.get(StructWithPrimitiveArray.class);
        StructWithPrimitiveArray s = serializer.load(buffer);
        assertEquals(25, s.array.length);
        serializer.save(buffer, s);
        //ok, no error
    }

    @Test
    public void structWithByteArray() {
        Serializer<StructWithByteArray> serializer = Serializers.INSTANCE.get(StructWithByteArray.class);
        StructWithByteArray struct = serializer.load(buffer);
        struct.bytes[1] = 4;
        serializer.save(buffer, struct);
        //ok, no error
    }

    @Test(expected = SerializationException.class)
    public void structWithByteArray_changeLength() {
        Serializer<StructWithByteArray> serializer = Serializers.INSTANCE.get(StructWithByteArray.class);
        StructWithByteArray struct = serializer.load(buffer);
        struct.bytes= new byte[100];
        serializer.save(buffer, struct);
        //ok, no error
    }

    @Test
    public void structWithCustomSerializer() {
        Serializer<StructWithSerializer> serializer = Serializers.INSTANCE.get(StructWithSerializer.class);
        StructWithSerializer s = serializer.load(buffer);
        assertTrue(s.loadCalled);
        serializer.save(buffer, s);
        assertTrue(s.saveCalled);
    }

    @Test(expected = SerializationException.class)
    public void structSmallForArray() {
        Serializer<SmallStructForArray> serializer = Serializers.INSTANCE.get(SmallStructForArray.class);
        SmallStructForArray ar = serializer.load(buffer);
    }

    @Limited(size = 100)
    public static class StructWithArray {
        public Integer[] array;
    }
    
    public static class InnerStruct {
        @Limited(size = 50)
        public String name;
    }
    
    @Limited(size = 100)
    public static class StructWithStructArray {
        public InnerStruct[] structs;
    }

    @Limited(size = 100)
    public static class StructWithPrimitiveArray {
        public Integer[] array;
    }

    @Limited(size = 100)
    public static class StructWithByteArray {
        public int val1;
        public byte[] bytes;
    }

    @Limited(size = 3)
    public static class SmallStructForArray {
        public Integer[] array;
    }

    public static class StructWithSerializer implements Serializer<StructWithSerializer> {
        public boolean saveCalled = false;
        public boolean loadCalled = false;

        @Override
        public void save(ByteBuffer buffer, StructWithSerializer instance) {
            saveCalled = true;
        }

        @Override
        public StructWithSerializer load(ByteBuffer buffer) {
            loadCalled = true;
            return this;
        }

        @Override
        public int getSize() {
            return 10;
        }
    }
    
}
