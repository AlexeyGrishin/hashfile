package io.github.alexeygrishin;

public class InvalidSyntax extends RuntimeException {
    public InvalidSyntax() {
    }

    public InvalidSyntax(String message) {
        super(message);
    }

    public InvalidSyntax(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidSyntax(Throwable cause) {
        super(cause);
    }
}
