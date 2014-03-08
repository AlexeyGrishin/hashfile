package io.github.alexeygrishin.btree;

import io.github.alexeygrishin.common.CacheBase;

public class TreeNamesCache implements TreeNameHelper {

    private TreeNameHelper wrapped;
    private CacheBase<Long, String> cache;

    public TreeNamesCache(final TreeNameHelper wrapped, int maxSize, int longNamesInitialCount) {
        this.wrapped = wrapped;
        this.cache = new CacheBase<Long, String>(maxSize, longNamesInitialCount) {
            @Override
            protected String getFromSource(Long key) {
                return wrapped.getFullName(key);
            }

            @Override
            protected int getSize(String element) {
                return element.length() * 2;
            }

            @Override
            protected void free(Long key, String element) {
                //nothing special
            }
        };
    }

    @Override
    public String getFullName(long dataId) {
        return cache.get(dataId);
    }

    @Override
    public String truncate(String fullName, int targetLen) {
        return wrapped.truncate(fullName, targetLen);
    }
}
