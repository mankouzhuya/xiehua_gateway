package com.xiehua.fun;

@FunctionalInterface
public interface UncheckedFunctionArg2<T, R> {
    R apply(T t) throws Exception;
}