package io.github.alexeygrishin.common;

public class Check {
    public static <T> T notNull(T... objects) {
        for (T obj: objects) {
            if (obj != null) {
                return obj;
            }
        }
        throw new NullPointerException();
    }

    public static <T extends Number> T positive(T numLike, String name) {
        if (numLike.longValue() <= 0) throw new IllegalArgumentException("Value '" + name + "' shall be > 0");
        return numLike;
    }

}
