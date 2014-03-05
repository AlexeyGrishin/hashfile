package io.github.alexeygrishin.blockalloc.serializers;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Limited {
    public int size();
}
