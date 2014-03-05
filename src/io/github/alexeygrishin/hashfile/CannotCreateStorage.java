package io.github.alexeygrishin.hashfile;

public class CannotCreateStorage extends RuntimeException {

    public CannotCreateStorage() {
    }

    public CannotCreateStorage(String message) {
        super(message);
    }

    public CannotCreateStorage(String message, Throwable cause) {
        super(message, cause);
    }

    public CannotCreateStorage(Throwable cause) {
        super(cause);
    }
}
