package io.github.alexeygrishin.common;


public class SourceInaccessible extends RuntimeException {
    public SourceInaccessible() {
    }

    public SourceInaccessible(String message) {
        super(message);
    }

    public SourceInaccessible(String message, Throwable cause) {
        super(message, cause);
    }

    public SourceInaccessible(Throwable cause) {
        super(cause);
    }
}
