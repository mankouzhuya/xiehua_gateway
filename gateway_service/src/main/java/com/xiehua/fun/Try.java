package com.xiehua.fun;

import java.util.Objects;
import java.util.function.Function;

public class Try {


    public static <R> Function of(UncheckedFunction1<R> mapper) {
        Objects.requireNonNull(mapper);
        return t -> {
            try {
                return mapper.apply();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    public static <T, R> Function<T, R> of(UncheckedFunctionArg2<T, R> mapper) {
        Objects.requireNonNull(mapper);
        return t -> {
            try {
                return mapper.apply(t);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
    }
}