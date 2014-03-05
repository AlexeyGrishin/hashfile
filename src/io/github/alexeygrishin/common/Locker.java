package io.github.alexeygrishin.common;

import java.util.concurrent.locks.Lock;

/**
 * Just wrapper on {@link java.util.concurrent.locks.Lock} to use in try(resources...) expression.
 */
public class Locker implements AutoCloseable {

    private Lock lock;

    public Locker(Lock lock) {
        this.lock = lock;
        lock.lock();
    }

    public void close() {
        lock.unlock();
    }
}
