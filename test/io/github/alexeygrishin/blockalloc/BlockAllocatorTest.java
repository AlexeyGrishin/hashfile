package io.github.alexeygrishin.blockalloc;

import io.github.alexeygrishin.TestBaseWithCounter;
import io.github.alexeygrishin.blockalloc.serializers.SerializationException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class BlockAllocatorTest extends TestBaseWithCounter {

    private BlockAllocator allocator;

    @Before
    public void setup() {
        allocator = new BlockAllocator(counter, Struct_10Bytes.BLOCK_SIZE);
    }

    @Test
    public void allocate() {
        int newBlock = allocator.allocate();
        assertReadsWrites(0, 0, 1);
    }

    @Test
    public void get() {
        int newBlock = allocator.allocate();
        counter.resetCounters();
        Struct_10Bytes struct = allocator.get(newBlock, Struct_10Bytes.class);
        assertNotNull(struct);
        assertReadsWrites(1, 0, 0);
    }

    @Test(expected = SerializationException.class)
    public void get_bigStruct() {
        int newBlock = allocator.allocate();
        allocator.get(newBlock, Struct_11Bytes.class);
    }

    @Test
    public void saveModifications() {
        int newBlock = allocator.allocate();
        Struct_10Bytes struct = allocator.get(newBlock, Struct_10Bytes.class);

        counter.resetCounters();
        struct.value = 5;
        allocator.saveModifications(newBlock, struct);

        assertReadsWrites(0, 1, 0);
        assertEquals(5, allocator.get(newBlock, Struct_10Bytes.class).value);
    }

    @Test
    public void getForModification() {
        int newBlock = allocator.allocate();
        counter.resetCounters();
        try (BlockToModify<Struct_10Bytes> block = allocator.getToModify(newBlock, Struct_10Bytes.class)) {
            block.getBlock().value = 10;
        }

        assertReadsWrites(1, 1, 0);
        assertEquals(10, allocator.get(newBlock, Struct_10Bytes.class).value);
    }

    @Test
    public void allocateForModification() {
        int blockId = -1;
        try (BlockToModify<Struct_10Bytes> block = allocator.allocateToModify(Struct_10Bytes.class)) {
            blockId = block.getBlockId();
            block.getBlock().value = 55;
        }
        assertReadsWrites(1, 1, 1);
        assertEquals(55, allocator.get(blockId, Struct_10Bytes.class).value);
    }
}
