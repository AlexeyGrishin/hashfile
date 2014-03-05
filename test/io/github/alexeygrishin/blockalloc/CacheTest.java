package io.github.alexeygrishin.blockalloc;

import io.github.alexeygrishin.TestBaseWithCounter;
import org.junit.Before;
import org.junit.Test;

public class CacheTest extends TestBaseWithCounter {


    private Cache allocator;
    private int block1, block2, block3;

    @Before
    public void setup() {
        allocator = new Cache(new BlockAllocator(counter, Struct.BLOCK_SIZE), 2);
        block1 = allocator.allocate();
        block2 = allocator.allocate();
        block3 = allocator.allocate();
        allocator.reset();
        counter.resetCounters();
    }

    @Test
    public void subsequentRead() {
        allocator.get(block1, Struct.class);
        allocator.get(block2, Struct.class);
        assertReadsWrites(2, 0, 0);
        counter.resetCounters();
        allocator.get(block1, Struct.class);
        allocator.get(block2, Struct.class);
        assertReadsWrites(0, 0, 0);
    }

    @Test
    public void readNewBlockWithFullCache() {
        allocator.get(block1, Struct.class);
        allocator.get(block2, Struct.class);
        counter.resetCounters();
        allocator.get(block3, Struct.class);
        assertReadsWrites(1, 0, 0);
    }

    @Test
    public void readNewBlockWithFullCache_lru() {
        allocator.get(block1, Struct.class);
        allocator.get(block2, Struct.class);
        allocator.get(block1, Struct.class);

        allocator.get(block3, Struct.class);
        counter.resetCounters();
        //block1 shall remain as recently used
        allocator.get(block1, Struct.class);
        assertReadsWrites(0, 0, 0);
        counter.resetCounters();
        //block2 shall be loaded
        allocator.get(block2, Struct.class);
        assertReadsWrites(1, 0, 0);

    }

    @Test
    public void subsequentWrites() {
        Struct o = allocator.get(block1, Struct.class);
        counter.resetCounters();
        allocator.saveModifications(block1, o);
        allocator.saveModifications(block1, o);
        allocator.saveModifications(block1, o);
        assertReadsWrites(0, 0, 0);
    }

    @Test
    public void subsequentWrites_fullCache() {
        Struct o = allocator.get(block1, Struct.class);
        allocator.saveModifications(block1, o);
        allocator.saveModifications(block1, o);

        counter.resetCounters();
        allocator.get(block2, Struct.class);
        allocator.get(block3, Struct.class);
        assertReadsWrites(2, 1, 0);
    }

    @Test
    public void writes_saveOnClose() {
        Struct o = allocator.get(block1, Struct.class);
        allocator.saveModifications(block1, o);
        counter.resetCounters();

        allocator.close();
        assertReadsWrites(0, 1, 0);
    }

}
