package io.github.alexeygrishin.tool;

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;

public class ByteGeneratorCounterTest {

    @Test
    public void readBytes() throws IOException {
        InputStream is = new ByteGenerator(2);
        assertTrue(is.read() > 0);
        assertTrue(is.read() > 0);
        assertTrue(is.read() == -1);
    }

    @Test
    public void empty() throws IOException {
        InputStream is = new ByteGenerator(0);
        assertTrue(is.read() == -1);
    }

    @Test
    public void reset() throws IOException {
        InputStream is = new ByteGenerator(1);
        assertTrue(is.read() > 0);
        assertTrue(is.read() == -1);
        is.reset();
        assertTrue(is.read() > 0);
        assertTrue(is.read() == -1);

    }

    @Test
    public void countBytes() throws IOException {
        ByteCounter bc = new ByteCounter();
        bc.write(new byte[] {1,2,3});
        assertEquals(3, bc.getCounted());
    }
}
