package io.github.alexeygrishin.common;

import java.io.*;

public class RealSource implements Source {
    private final File file;
    private final String relativePath;

    public RealSource(File file, String relativePath) {
        this.file = file;
        this.relativePath = relativePath;
    }

    @Override
    public String toKey() {
        return relativePath;
    }

    @Override
    public InputStream openInputStream() {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new SourceInaccessible(e);
        }
    }

    @Override
    public OutputStream openOutputStream() {
        try {
            return new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            throw new SourceInaccessible(e);
        }
    }
}
