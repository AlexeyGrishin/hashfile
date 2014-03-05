package io.github.alexeygrishin.bytestorage;

import java.io.IOException;

public class StorageFault extends RuntimeException {
    public StorageFault(IOException e) {
        super(e);
    }
}
