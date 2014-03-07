package io.github.alexeygrishin.hashfile;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

public interface NamedStorage extends Iterable<String>, AutoCloseable {

    //
    boolean getInto(String key, OutputStream stream);

    void saveFrom(String key, InputStream stream);

    boolean contains(String key);

    void delete(String key);

    Iterator<String> iterator();

    void close();

    void cloneTo(NamedStorage storage);
}
