package io.github.alexeygrishin.btree;

import io.github.alexeygrishin.hashfile.btreebased.DataException;

public class NoSuchElement extends DataException {
    public NoSuchElement() {
    }

    public NoSuchElement(String message) {
        super(message);
    }

    public NoSuchElement(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSuchElement(Throwable cause) {
        super(cause);
    }
}
