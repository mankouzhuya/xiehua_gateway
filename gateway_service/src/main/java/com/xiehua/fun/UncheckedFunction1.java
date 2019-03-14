package com.xiehua.fun;

@FunctionalInterface
public interface UncheckedFunction1<R> {
    R apply() throws Exception;
}