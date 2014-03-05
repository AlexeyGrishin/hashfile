package io.github.alexeygrishin.hashfile;

public class CannotLoadStorage extends RuntimeException {

    public CannotLoadStorage() {
    }

    public CannotLoadStorage(String message) {
        super(message);
    }

    public CannotLoadStorage(String message, Throwable cause) {
        super(message, cause);
    }

    public CannotLoadStorage(Throwable cause) {
        super(cause);
    }
}
