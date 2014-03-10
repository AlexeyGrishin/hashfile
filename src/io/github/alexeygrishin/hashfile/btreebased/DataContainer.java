package io.github.alexeygrishin.hashfile.btreebased;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Container for user data storage.
 * Note that container does not check correctness of provided block indexes, calling it with non-data block index may lead
 * to unpredictable behavior.
 *
 * Thread-safe, operations on different data blocks could be performed concurrently.
 */
public interface DataContainer {

    /**
     * Gets full name of data stored in specified block.
     * @param blockIdx
     * @return
     */
    String getFullName(int blockIdx);

    /**
     * Replaces data for the specified block. If new data larger/smaller than previously stored
     * then it allocates new blocks/frees old blocks if needed.
     * @param blockIdx block index
     * @param stream data source
     */
    void update(int blockIdx, InputStream stream);

    /**
     * Gets data from the specified block and passes it to the output stream
     * @param blockIdx block index
     * @param stream data sink
     */
    void select(int blockIdx, OutputStream stream);

    /**
     * Inserts data from provided stream and the data name into the storage.
     * This method may allocate any amount of blocks needed for both name and data. Their order is unknown and cannot
     * be calculated from returned first data block index.
     * @param fullName data name
     * @param stream stream
     * @return index of the first data block.
     */
    int insert(String fullName, InputStream stream);

    /**
     * Deletes all data starting from the provided block
     * @param blockIdx
     */
    void delete(int blockIdx);


}
