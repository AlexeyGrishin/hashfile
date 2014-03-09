package io.github.alexeygrishin.common;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base class for caching some key-based data. The cache is limited by size (in bytes) and works as LRU.
 * @param <K> key type
 * @param <T> value type.
 */
public abstract class CacheBase<K, T> {

    private Map<K, T> map;
    private long maxSizeInBytes;
    private long cacheSize;

    /**
     *
     * @param maxSizeInBytes max size in bytes for cache. Shall be much bigger than one element size.
     * @param initialCapacity shall be closer to max elements count as possible
     */
    public CacheBase(long maxSizeInBytes, int initialCapacity) {
        this.maxSizeInBytes = maxSizeInBytes;
        this.map = new LinkedHashMap<>(initialCapacity, 0.75f, true);
    }


    public final synchronized T get(K key) {
        T element = map.get(key);
        if (element == null) {
            element = getFromSource(key);
            addToCache(key, element);
        }
        return element;
    }

    private void addToCache(K key, T element) {
        int elementSize = getSize(key, element);
        if (isCacheFull()) {
            Iterator<Map.Entry<K, T>> iter = map.entrySet().iterator();
            Map.Entry<K, T> oldPair = iter.next();
            free(oldPair.getKey(), oldPair.getValue());
            iter.remove();
            cacheSize -= elementSize;
        }
        if (map.put(key, element) == null) {
            cacheSize += elementSize;
        }
    }


    public final synchronized void put(K key, T value) {
        addToCache(key, value);
    }

    public final synchronized void reset() {
        for (Map.Entry<K, T> entry: map.entrySet()) {
            free(entry.getKey(), entry.getValue());
        }
        map.clear();
        cacheSize = 0;
    }




    protected final boolean isCacheFull() {
        return isCacheLimitReached(map.size()) || cacheSize >= maxSizeInBytes;
    }

    /**
     * Override if cache shall be limited by count of element.
     * @param cachedElements amount of elements
     * @return true if cache is full, false otherwise
     */
    protected boolean isCacheLimitReached(int cachedElements) {
        return false;
    }

    /**
     *
     * @param key
     * @return element by key from cached source
     */
    protected abstract T getFromSource(K key);

    /**
     *
     * @param key
     * @param element
     * @return element's size in bytes
     */
    protected abstract int getSize(K key, T element);

    /**
     * Called when cache removes element from itself due to cache fullness.
     * @param key
     * @param element
     */
    protected abstract void free(K key, T element);


}
