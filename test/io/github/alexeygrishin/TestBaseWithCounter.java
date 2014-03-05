package io.github.alexeygrishin;

import io.github.alexeygrishin.bytestorage.Counter;
import io.github.alexeygrishin.bytestorage.MemoryContainer;
import org.junit.Before;

import static org.junit.Assert.assertEquals;

public class TestBaseWithCounter {
    protected Counter counter;

    @Before
    public void setupCounter() {
        counter = new Counter(new MemoryContainer());
    }

    protected void assertReadsWrites(int reads, int writes, int appends) {
        assertEquals("Amount of disk reads does not match",     reads,   counter.getReads());
        assertEquals("Amount of disk writes does not match",    writes,  counter.getWrites());
        assertEquals("Amount of disk appends does not match",   appends, counter.getAppends());
    }
}
