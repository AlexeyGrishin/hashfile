package io.github.alexeygrishin.blockalloc;

import io.github.alexeygrishin.common.CacheBase;

/**
 * Caching decorator for any allocator. Keeps recently accessed blocks in memory. Also keep all changes to blocks
 * and passes them to decorated allocator if cache is full and some changed element need to be moved out from cache,
 * or when {@link #close()} or {@link #reset()} are called
 */
public class Cache implements Allocator {
    private final Allocator inner;
    private CacheBase<CacheKey, CacheEntry> cache;

    public Cache(final Allocator inner, final int maxCacheLength) {
        this.inner = inner;
        long maxCacheSize = (long)maxCacheLength * inner.getBlockSize();
        this.cache = new CacheBase<CacheKey, CacheEntry>(maxCacheSize, maxCacheLength*2) {

            @Override
            protected boolean isCacheLimitReached(int cachedElements) {
                return cachedElements >= maxCacheLength;
            }

            @Override
            protected CacheEntry getFromSource(CacheKey key) {
                return new CacheEntry(inner.get(key.blockId, key.kls), false);
            }

            @Override
            protected int getSize(CacheEntry element) {
                return inner.getBlockSize();
            }

            @Override
            protected void free(CacheKey key, CacheEntry element) {
                if (element.openedForWrite) {
                    inner.saveModifications(key.blockId, element.block);
                }
            }
        };
    }

    @Override
    public int allocate() {
        return inner.allocate();
    }

    @Override
    public int getBlockSize() {
        return inner.getBlockSize();
    }

    @Override
    public int getBlocksCount() {
        return inner.getBlocksCount();
    }

    @Override
    public void free(int blockId) {
        inner.free(blockId);
    }

    @Override
    public <T> T get(int blockId, Class<T> kls) {
        return kls.cast(cache.get(new CacheKey(blockId, kls)).block);
    }

    @Override
    public void saveModifications(int blockId, Object data) {
        cache.put(new CacheKey(blockId, data.getClass()), CacheEntry.write(data));
    }

    @Override
    public <T> BlockToModify<T> getToModify(int blockId, Class<T> kls) {
        CacheEntry entry = cache.get(new CacheKey(blockId, kls));
        entry.openedForWrite = true;
        return new BlockToModify<T>(this, blockId, kls.cast(entry.block));
    }

    @Override
    public <T> BlockToModify<T> allocateToModify(Class<T> kls) {
        return getToModify(allocate(), kls);
    }

    @Override
    public void close() {
        reset();
        inner.close();
    }

    public void reset() {
        cache.reset();
    }

    static class CacheKey {
        private int blockId;
        private Class<?> kls;

        CacheKey(int blockId, Class<?> kls) {
            this.blockId = blockId;
            this.kls = kls;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CacheKey cacheKey = (CacheKey) o;

            if (blockId != cacheKey.blockId) return false;
            if (!kls.equals(cacheKey.kls)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = blockId;
            result = 31 * result + kls.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return blockId + "(as " + kls.getSimpleName() + ")";
        }
    }

    static class CacheEntry {
        CacheEntry(Object block, boolean openedForWrite) {
            this.block = block;
            this.openedForWrite = openedForWrite;
        }

        static CacheEntry read(Object block) {
            return new CacheEntry(block, false);
        }

        static CacheEntry write(Object block) {
            return new CacheEntry(block, true);
        }

        Object block;
        boolean openedForWrite;
    }

}
