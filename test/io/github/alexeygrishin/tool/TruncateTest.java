package io.github.alexeygrishin.tool;

import io.github.alexeygrishin.btree.Truncate;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TruncateTest {

    @Test
    public void leading_shorterThanLimit() {
        assertEquals("test", Truncate.leading("test", 5));
    }

    @Test
    public void leading_sameAsLimit() {
        assertEquals("test", Truncate.leading("test", 4));
    }

    @Test
    public void leading_longerThanLimit() {
        assertEquals("tes", Truncate.leading("test", 3));
    }

    @Test
    public void trailing_shorterThanLimit() {
        assertEquals("test", Truncate.trailing("test", 5));
    }

    @Test
    public void trailing_sameAsLimit() {
        assertEquals("test", Truncate.trailing("test", 4));
    }

    @Test
    public void trailing_longerThanLimit() {
        assertEquals("est", Truncate.trailing("test", 3));
    }
}
