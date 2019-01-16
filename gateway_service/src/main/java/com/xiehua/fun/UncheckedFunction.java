package com.xiehua.fun;

@FunctionalInterface
public interface UncheckedFunction<R> {
    R apply() throws Exception;
}