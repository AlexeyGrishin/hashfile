package io.github.alexeygrishin.tool;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestTool {


    public static String randomString(int len) {
        StringBuilder bld = new StringBuilder();
        for (int i = 0; i < len; i++) {
            bld.append(randomChar());
        }
        return bld.toString();
    }

    public static List<String> randomStrings(int count, int len) {
        List<String> strings = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            strings.add(randomString(len));
        }
        return strings;
    }

    public static InputStream generateData(int len) {
        return new ByteGenerator(len);
    }


    public  static long delta(long time) {
        return now() - time;
    }

    public static long now() {
        return System.nanoTime();
    }

    public static char randomChar() {
        return (char)(Math.random() * ('z' - 'a') + 'a');
    }


    public static List<String> iteratorToList(Iterable<String> tree) {
        List<String> actual = new ArrayList<>();
        for (String key: tree) {
            actual.add(key);
        }
        Collections.sort(actual);
        return actual;
    }
}
