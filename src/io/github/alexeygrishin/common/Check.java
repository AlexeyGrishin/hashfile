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

    public static void arguments(boolean result, String message) {
        if (!result) {
            throw new IllegalArgumentException(message);
        }
    }

    public static <T extends Number> T positive(T numLike, String name) {
        if (numLike.longValue() <= 0) throw new IllegalArgumentException("Value '" + name + "' shall be > 0");
        return numLike;
    }

    public static int safeInt(long val) {
        if (val > Integer.MAX_VALUE || val < Integer.MIN_VALUE) {
            throw new IllegalArgumentException("Cannot cast " + val + " to int");
        }
        return (int)val;
    }

}
