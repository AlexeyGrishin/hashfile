package io.github.alexeygrishin.common;

public class InvalidPointer extends RuntimeException {
    public InvalidPointer() {
    }

    public InvalidPointer(String message) {
        super(message);
    }

    public InvalidPointer(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPointer(Throwable cause) {
        super(cause);
    }
}
