package io.github.alexeygrishin.common;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Provides ability to lock by some key.
 * It dynamically creates ReadWriteLock and associated it with key on {@link #lockRead(Object)}/{@link #lockWrite(Object)} call.
 *
 * Usage example:
 * <code>
 *     LockMap<Integer> lockMap = new LockMap<>();
 *     try (LockMap.AutoLock lock = lockMap.lockWrite(1)) {
 *         ...
 *     }
 * </code>
 *
 * @param <T>
 */
public class LockMap<T> {

    private final Map<T, LockEntry> map = new HashMap<>();
    private final Deque<ReadWriteLock> freeLocks = new LinkedList<>();
    private final Object guard = new Object();

    class LockEntry {
        ReadWriteLock lock;
        int usages = 0;

        LockEntry(ReadWriteLock lock) {
            this.lock = lock;
        }
    }


    public AutoLock lockRead(T key) {
        ReadWriteLock lock = getLock(key);
        return new AutoLock(key, lock.readLock());
    }

    private ReadWriteLock getLock(T key) {
        LockEntry entry;
        synchronized (guard) {
            entry = map.get(key);
            if (entry == null) {
                entry = new LockEntry(freeLocks.isEmpty() ? new ReentrantReadWriteLock() : freeLocks.pop());
                map.put(key, entry);
            }
            entry.usages++;
        }
        return entry.lock;
    }

    public AutoLock lockWrite(T key) {
        ReadWriteLock lock = getLock(key);
        return new AutoLock(key, lock.writeLock());
    }


    public class AutoLock implements AutoCloseable {

        private final Lock lock;
        private final T key;

        public AutoLock(T key, Lock lock) {
            lock.lock();
            this.lock = lock;
            this.key = key;
        }

        public void close() {
            lock.unlock();
            synchronized (guard) {
                LockEntry entry = map.get(key);
                if (entry != null) {
                    entry.usages--;
                    if (entry.usages == 0) {
                        map.remove(key);
                        freeLocks.push(entry.lock);
                    }
                }
                else {
                    //TODO: ? shall never happen
                }
            }
        }
    }
}
