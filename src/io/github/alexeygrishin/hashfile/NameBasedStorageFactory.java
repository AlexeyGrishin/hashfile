package io.github.alexeygrishin.hashfile;

import io.github.alexeygrishin.btree.KeyTruncateMethod;

public interface NameBasedStorageFactory {
    NamedStorage create(String filePath, Integer blockSizeK, Integer cacheLimit, KeyTruncateMethod part);

    NamedStorage load(String filePath);

    NamedStorage create(String filePath);

    void truncate(String filePath);
}
