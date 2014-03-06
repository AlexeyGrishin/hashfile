package io.github.alexeygrishin.common;

/**
 * For pointing blocks the int type is used. Any negative value is invalid pointer and shall be recognized as null-pointer.
 * Note: even the 0 is the valid pointer, in most cases it is not true and may be caused by non-initialized struct (as int's default is 0).
 * So methods {@link #isValidNext(long)} and {@link #isNullNext(long)}  recognize 0 as invalid pointer.
 */
public class Pointer {

    public static final int NULL_PTR = -1;

    public static boolean isValid(long pointer) {
        return pointer >= 0;
    }

    public static boolean isNull(long pointer) {
        return pointer < 0;
    }

    public static boolean isValidNext(long pointer) {
        return isValid(pointer) && pointer != 0;
    }

    public static boolean isNullNext(long pointer) {
        return isNull(pointer) || pointer == 0;
    }

}
