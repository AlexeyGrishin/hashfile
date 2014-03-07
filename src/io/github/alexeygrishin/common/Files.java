package io.github.alexeygrishin.common;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Represents file system
 */
public interface Files {
    String[] resolveIds(String path);
    String toKey(String id);
    InputStream getInputStream(String id);
    OutputStream getOutputStream(String id);

}
