package io.github.alexeygrishin.btree;

public enum KeyTruncateMethod {
    LEADING(1), TRAILING(2);

    private int value;

    KeyTruncateMethod(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static KeyTruncateMethod valueOf(int truncateMethod) {
        for (KeyTruncateMethod part: values()) {
            if (part.value == truncateMethod) {
                return part;
            }
        }
        throw new IllegalArgumentException("Unknown truncate method " + truncateMethod);
    }
}
