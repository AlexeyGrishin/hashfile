package io.github.alexeygrishin.blockalloc;

import io.github.alexeygrishin.TestBaseWithCounter;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MultiBlockAllocatorTest extends TestBaseWithCounter {

    private MultiBlockAllocator multiBlock;
    private BlockAllocator allocator;

    private MultiBlockAllocator create(int innerBlock, int outerBlock) {
        return multiBlock = new MultiBlockAllocator(allocator = new BlockAllocator(counter, innerBlock), outerBlock);
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_smallerBlockSize() {
        create(256, 128);
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_exactBlock() {
        create(256, 256);
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_fractBlock() {
        create(256, 1025);
    }

    @Test
    public void create_intBlocks() {
        create(256, 1024);
    }

    @Test
    public void allocate() {
        create(256, 1024);
        assertEquals(0, multiBlock.allocate());
        assertEquals(4, multiBlock.allocate());
        assertReadsWrites(0, 0, 2);
        assertEquals(8, allocator.getBlocksCount());
    }

    @Test
    public void saveModifications_structBiggerThanInnerBlock() {
        create(10, 40);
        try (BlockToModify<Struct_11Bytes> block = multiBlock.allocateToModify(Struct_11Bytes.class)) {
            block.getBlock().value = 4;
        }
        assertReadsWrites(1, 1, 1);
        assertEquals(4, allocator.getBlocksCount());
    }

    @Test
    public void saveModifications_structSameAsOuterBlock() {
        create(10, 20);
        try (BlockToModify<Struct_20Bytes> block = multiBlock.allocateToModify(Struct_20Bytes.class)) {
            block.getBlock().value3 = 40;
        }
        assertReadsWrites(1, 1, 1);
        assertEquals(2, allocator.getBlocksCount());
    }
}
