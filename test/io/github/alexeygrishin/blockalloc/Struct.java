package io.github.alexeygrishin.blockalloc;

import io.github.alexeygrishin.blockalloc.serializers.Limited;

@Limited(size = Struct.BLOCK_SIZE)
public class Struct {
    public static final int BLOCK_SIZE = 10;
    public long value;
}
