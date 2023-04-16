package io.kineolyan.stagelock.internal;

import lombok.val;

import java.time.Duration;

public interface GateTaker {

    boolean tryAcquireInterruptibly(Duration timeout) throws InterruptedException;

    default boolean tryAcquire(final Duration timeout) {
        try {
            return tryAcquireInterruptibly(timeout);
        } catch (final InterruptedException e) {
            throw new IllegalStateException("Interrupted while waiting for the lock", e);
        }
    }

    default void acquire() {
        val result = tryAcquire(null);
        assert result : "Not acquired";
    }

    default void acquireInterruptibly() throws InterruptedException {
        val result = tryAcquireInterruptibly(null);
        assert result : "Not acquired";
    }

    void release();

}