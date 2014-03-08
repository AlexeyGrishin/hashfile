package io.github.alexeygrishin.blockalloc.serializers;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Limits size for class or {@link String} field. Is used for serialization.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Limited {
    /**
     *
     * @return size in bytes (not characters!) for whole class or String field.
     */
    public int size();
}
