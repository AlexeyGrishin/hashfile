package io.github.alexeygrishin.hashfile;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

/**
 * Is a storage which allows to save/get data by string name.
 */
public interface NamedStorage extends Iterable<String>, AutoCloseable {

    /**
     * Gets data stored by specified key and puts it to the provided stream.
     * Does not close given stream.
     * @param key data key
     * @param stream stream to process data
     * @return true if data was found, false otherwise
     */
    boolean getInto(String key, OutputStream stream);

    /**
     * Saves data from stream using provided key. If there was data for this key it will be overwritten with new data.
     * Does not close given stream.
     * @param key data key
     * @param stream stream to get data from
     */
    void saveFrom(String key, InputStream stream);

    /**
     * Checks that there is data stored for provided key
     * @param key data key
     * @return true if there is data for this key, false otherwise
     */
    boolean contains(String key);

    /**
     * Deletes key and associated data from storage. Does nothing if there is no such key in storage
     * @param key
     */
    void delete(String key);

    /**
     *
     * @return iterator for all keys. Order of keys is unknown.
     */
    Iterator<String> iterator();

    /**
     * Closes storage and flushes all changes on disk.
     */
    void close();

    /**
     * Copies all data to the specified storage
     * @param storage
     */
    void cloneTo(NamedStorage storage);
}
