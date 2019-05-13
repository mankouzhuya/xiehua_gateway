package com.xiehua.fun;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class Try {


    public static <R> Function of_f(UncheckedFunction1<R> mapper) {
        Objects.requireNonNull(mapper);
        return t -> {
            try {
                return mapper.apply();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    public static <T, R> Function<T, R> of_f(UncheckedFunction2<T, R> mapper) {
        Objects.requireNonNull(mapper);
        return t -> {
            try {
                return mapper.apply(t);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    public static <T> Predicate<? super T> of_p(UncheckedPredicate<T> predicate) {
        Objects.requireNonNull(predicate);
        return t -> {
            try {
                return predicate.test(t);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    public static <T> Consumer<T> of_c(UncheckedConsumer<T> consumer) {
        Objects.requireNonNull(consumer);
        return t -> {
            try {
                consumer.accept(t);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
    }

}