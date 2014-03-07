package io.github.alexeygrishin.common;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Represents file system
 */
public interface Files {
    Source[] getSources(String path);
}
