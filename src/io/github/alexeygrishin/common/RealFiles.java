package io.github.alexeygrishin.common;

import java.io.*;

public class RealFiles implements Files {
    @Override
    public String toKey(String id) {
        return new File(id).getName();
    }

    @Override
    public InputStream getInputStream(String id) {
        try {
            return new FileInputStream(id);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);  //TODO
        }
    }

    @Override
    public OutputStream getOutputStream(String id) {
        try {
            return new FileOutputStream(id);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
