package io.github.alexeygrishin.btree;

public class Truncate {

    //TODO: test
    public static String leading(String str, int maxLen) {
        return str.length() <= maxLen ? str : str.substring(0, maxLen);
    }

    public static String trailing(String str, int maxLen) {
        return str.length() <= maxLen ? str : str.substring(str.length() - maxLen);
    }

    public static String part(String str, int maxLen, KeyTruncateMethod part) {
        switch (part) {
            case TRAILING: return trailing(str, maxLen);
            case LEADING: return leading(str, maxLen);
        }
        throw new IllegalArgumentException("Unknown key part " + part);
    }
}
