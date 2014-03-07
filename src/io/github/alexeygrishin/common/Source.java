package io.github.alexeygrishin.common;

import java.io.InputStream;
import java.io.OutputStream;

public interface Source {
    String toKey();
    InputStream openInputStream();
    OutputStream openOutputStream();
}
