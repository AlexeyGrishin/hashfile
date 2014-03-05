package io.github.alexeygrishin.tool;

import java.io.IOException;
import java.io.OutputStream;

public class ByteCounter extends OutputStream {

    private long counted = 0;

    @Override
    public void write(int b) throws IOException {
        counted++;
    }

    public void reset() {
        counted = 0;
    }

    public long getCounted() {
        return counted;
    }
}
