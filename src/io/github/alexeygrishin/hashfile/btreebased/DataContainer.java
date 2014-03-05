package io.github.alexeygrishin.hashfile.btreebased;

import java.io.InputStream;
import java.io.OutputStream;

public interface DataContainer {

    String getFullName(int blockIdx);

    void update(int blockIdx, InputStream stream);

    void select(int blockIdx, OutputStream stream);

    int insert(String fullName, InputStream stream);

    void delete(int blockIdx);


}
