package com.alibaba.ttl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import static com.alibaba.ttl.TransmittableThreadLocal.Transmitter.*;

/**
 * {@link TtlCallable} decorate {@link Callable}, so as to get {@link TransmittableThreadLocal}
 * and transmit it to the time of {@link Callable} execution, needed when use {@link Callable} to thread pool.
 * <p>
 * Use factory method {@link #get(Callable)} to get decorated instance.
 *
 * @author Jerry Lee (oldratlee at gmail dot com)
 * @see com.alibaba.ttl.threadpool.TtlExecutors
 * @see java.util.concurrent.Executor
 * @see java.util.concurrent.ExecutorService
 * @see java.util.concurrent.ThreadPoolExecutor
 * @see java.util.concurrent.ScheduledThreadPoolExecutor
 * @see java.util.concurrent.Executors
 * @see java.util.concurrent.CompletionService
 * @see java.util.concurrent.ExecutorCompletionService
 * @since 0.9.0
 */
public final class TtlCallable<V> implements Callable<V>, TtlEnhanced {
    private final AtomicReference<Object> capturedRef;
    private final Callable<V> callable;
    private final boolean releaseTtlValueReferenceAfterCall;

    private TtlCallable(@Nonnull Callable<V> callable, boolean releaseTtlValueReferenceAfterCall) {
        this.capturedRef = new AtomicReference<Object>(capture());
        this.callable = callable;
        this.releaseTtlValueReferenceAfterCall = releaseTtlValueReferenceAfterCall;
    }

    /**
     * wrap method {@link Callable#call()}.
     */
    @Override
    public V call() throws Exception {
        Object captured = capturedRef.get();
        if (captured == null || releaseTtlValueReferenceAfterCall && !capturedRef.compareAndSet(captured, null)) {
            throw new IllegalStateException("TTL value reference is released after call!");
        }

        Object backup = replay(captured);
        try {
            return callable.call();
        } finally {
            restore(backup);
        }
    }

    @Nonnull
    public Callable<V> getCallable() {
        return callable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TtlCallable<?> that = (TtlCallable<?>) o;

        return callable.equals(that.callable);
    }

    @Override
    public int hashCode() {
        return callable.hashCode();
    }

    @Override
    public String toString() {
        return this.getClass().getName() + " - " + callable.toString();
    }

    /**
     * Factory method, wrap input {@link Callable} to {@link TtlCallable}.
     * <p>
     * This method is idempotent.
     *
     * @param callable input {@link Callable}
     * @return Wrapped {@link Callable}
     */
    public static <T> TtlCallable<T> get(Callable<T> callable) {
        return get(callable, false);
    }


    /**
     * Factory method, wrap input {@link Callable} to {@link TtlCallable}.
     * <p>
     * This method is idempotent.
     *
     * @param callable                          input {@link Callable}
     * @param releaseTtlValueReferenceAfterCall release TTL value reference after run, avoid memory leak even if {@link TtlRunnable} is referred.
     * @return Wrapped {@link Callable}
     */
    @Nullable
    public static <T> TtlCallable<T> get(@Nullable Callable<T> callable, boolean releaseTtlValueReferenceAfterCall) {
        return get(callable, releaseTtlValueReferenceAfterCall, false);
    }

    /**
     * Factory method, wrap input {@link Callable} to {@link TtlCallable}.
     * <p>
     * This method is idempotent.
     *
     * @param callable                          input {@link Callable}
     * @param releaseTtlValueReferenceAfterCall release TTL value reference after run, avoid memory leak even if {@link TtlRunnable} is referred.
     * @param idempotent                        is idempotent or not. {@code true} will cover up bugs! <b>DO NOT</b> set, only when you know why.
     * @return Wrapped {@link Callable}
     */
    @Nullable
    public static <T> TtlCallable<T> get(@Nullable Callable<T> callable, boolean releaseTtlValueReferenceAfterCall, boolean idempotent) {
        if (null == callable) return null;

        if (callable instanceof TtlEnhanced) {
            // avoid redundant decoration, and ensure idempotency
            if (idempotent) return (TtlCallable<T>) callable;
            else throw new IllegalStateException("Already TtlCallable!");
        }
        return new TtlCallable<T>(callable, releaseTtlValueReferenceAfterCall);
    }

    /**
     * wrap input {@link Callable} Collection to {@link TtlCallable} Collection.
     *
     * @param tasks task to be wrapped
     * @return Wrapped {@link Callable}
     */
    @Nonnull
    public static <T> List<TtlCallable<T>> gets(@Nullable Collection<? extends Callable<T>> tasks) {
        return gets(tasks, false, false);
    }

    /**
     * wrap input {@link Callable} Collection to {@link TtlCallable} Collection.
     *
     * @param tasks                             task to be wrapped
     * @param releaseTtlValueReferenceAfterCall release TTL value reference after run, avoid memory leak even if {@link TtlRunnable} is referred.
     * @return Wrapped {@link Callable}
     */
    @Nonnull
    public static <T> List<TtlCallable<T>> gets(@Nullable Collection<? extends Callable<T>> tasks, boolean releaseTtlValueReferenceAfterCall) {
        return gets(tasks, releaseTtlValueReferenceAfterCall, false);
    }

    /**
     * wrap input {@link Callable} Collection to {@link TtlCallable} Collection.
     *
     * @param tasks                             task to be wrapped
     * @param releaseTtlValueReferenceAfterCall release TTL value reference after run, avoid memory leak even if {@link TtlRunnable} is referred.
     * @param idempotent                        is idempotent or not. {@code true} will cover up bugs! <b>DO NOT</b> set, only when you know why.
     * @return Wrapped {@link Callable}
     */
    @Nonnull
    public static <T> List<TtlCallable<T>> gets(@Nullable Collection<? extends Callable<T>> tasks, boolean releaseTtlValueReferenceAfterCall, boolean idempotent) {
        if (null == tasks) return Collections.emptyList();

        List<TtlCallable<T>> copy = new ArrayList<TtlCallable<T>>();
        for (Callable<T> task : tasks) {
            copy.add(TtlCallable.get(task, releaseTtlValueReferenceAfterCall, idempotent));
        }
        return copy;
    }

    /**
     * Unwrap {@link TtlCallable} to the original/underneath one.
     * <p>
     * this method is {@code null}-safe, when input {@code Callable} parameter is {@code null}, return {@code null};
     * if input {@code Callable} parameter is not a {@link TtlCallable} just return input {@code Callable}.
     * <p>
     * so {@code TtlCallable.unwrap(TtlCallable.get(callable))} will always return the same input {@code callable} object.
     *
     * @see #get(Callable)
     * @since 2.10.2
     */
    @Nullable
    public static <T> Callable<T> unwrap(@Nullable Callable<T> callable) {
        if (!(callable instanceof TtlCallable)) return callable;
        else return ((TtlCallable<T>) callable).getCallable();
    }

    /**
     * Unwrap {@link TtlCallable} to the original/underneath one.
     * <p>
     * Invoke {@link #unwrap(Callable)} for each element in input collection.
     * <p>
     * This method is {@code null}-safe, when input {@code Callable} parameter is {@code null}, return a empty list.
     *
     * @see #gets(Collection)
     * @see #unwrap(Callable)
     * @since 2.10.2
     */
    @Nonnull
    public static <T> List<Callable<T>> unwraps(@Nullable Collection<? extends Callable<T>> tasks) {
        if (null == tasks) return Collections.emptyList();

        List<Callable<T>> copy = new ArrayList<Callable<T>>();
        for (Callable<T> task : tasks) {
            if (!(task instanceof TtlCallable)) copy.add(task);
            else copy.add(((TtlCallable<T>) task).getCallable());
        }
        return copy;
    }
}
