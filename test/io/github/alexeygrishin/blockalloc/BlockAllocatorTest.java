package io.github.alexeygrishin.blockalloc;

import io.github.alexeygrishin.TestBaseWithCounter;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class BlockAllocatorTest extends TestBaseWithCounter {

    private BlockAllocator allocator;

    @Before
    public void setup() {
        allocator = new BlockAllocator(counter, Struct.BLOCK_SIZE);
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
        Struct struct = allocator.get(newBlock, Struct.class);
        assertNotNull(struct);
        assertReadsWrites(1, 0, 0);
    }

    @Test
    public void saveModifications() {
        int newBlock = allocator.allocate();
        Struct struct = allocator.get(newBlock, Struct.class);

        counter.resetCounters();
        struct.value = 5;
        allocator.saveModifications(newBlock, struct);

        assertReadsWrites(0, 1, 0);
        assertEquals(5, allocator.get(newBlock, Struct.class).value);
    }

    @Test
    public void getForModification() {
        int newBlock = allocator.allocate();
        counter.resetCounters();
        try (BlockToModify<Struct> block = allocator.getToModify(newBlock, Struct.class)) {
            block.getBlock().value = 10;
        }

        assertReadsWrites(1, 1, 0);
        assertEquals(10, allocator.get(newBlock, Struct.class).value);
    }

    @Test
    public void allocateForModification() {
        int blockId = -1;
        try (BlockToModify<Struct> block = allocator.allocateToModify(Struct.class)) {
            blockId = block.getBlockId();
            block.getBlock().value = 55;
        }
        assertReadsWrites(1, 1, 1);
        assertEquals(55, allocator.get(blockId, Struct.class).value);
    }
}
