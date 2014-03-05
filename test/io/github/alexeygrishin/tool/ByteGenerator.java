package io.github.alexeygrishin.tool;

import io.github.alexeygrishin.tool.TestTool;

import java.io.IOException;
import java.io.InputStream;

public class ByteGenerator extends InputStream {
    private long maxLength, remaining;

    public ByteGenerator(long maxLength) {
        this.maxLength = maxLength;
        this.remaining = maxLength;
    }

    @Override
    public int read() throws IOException {
        return remaining-->0 ? TestTool.randomChar() : -1;
    }

    public void reset() {
        this.remaining = maxLength;
    }
}
