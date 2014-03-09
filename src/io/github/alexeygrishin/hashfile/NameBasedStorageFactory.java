package io.github.alexeygrishin.hashfile;

import io.github.alexeygrishin.btree.KeyTruncateMethod;

import java.io.PrintStream;

public interface NameBasedStorageFactory {
    /**
     * Creates name based storage in specified file
     * @param filePath path to file where data will be stored. Shall not exist.
     * @param dataBlockSizeK data block size in KBytes (used to store data, shall be closer to average data size).  Null means default value.
     * @param cacheSizeM cache size in MBytes. Null means default value
     * @param part how to truncate key. Null means default value
     * @return created storage
     * @throws CannotCreateStorage
     */
    NamedStorage create(String filePath, Integer dataBlockSizeK, Integer cacheSizeM, KeyTruncateMethod part);

    /**
     * Same as {@link #create(String, Integer, Integer, io.github.alexeygrishin.btree.KeyTruncateMethod)} called with all
     * nulls except filePath. Creates named storage with default params.
     * @param filePath
     * @return
     * @throws CannotCreateStorage
     */
    NamedStorage create(String filePath);

    /**
     * Loads previously created storage from the provided path if file exists there. If not - creates new one (see {@link #create(String)}
     * @param filePath
     * @return
     * @throws CannotLoadStorage
     */
    NamedStorage load(String filePath);

    /**
     * Optimizes storage's contents and reduces storage file size if possible. Storage shall exist on specified path.
     * @param filePath
     * @throws CannotLoadStorage
     */
    void truncate(String filePath);

    /**
     * Gets storage meta information and prints to provided stream
     * @param filePath
     * @param out
     * @throws CannotLoadStorage
     */
    void printInfo(String filePath, PrintStream out);
}
