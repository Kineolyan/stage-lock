package io.kineolyan.stagelock.internal;

import lombok.RequiredArgsConstructor;
import lombok.val;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

@RequiredArgsConstructor
public final class PassthroughLock implements Lock {

    private final AtomicInteger counter = new AtomicInteger(0);
    private final GateTaker lockStage;
    private final Runnable unlockStage;

    private final Object lock = new Object();


    @Override
    public void lock() {
        synchronized (this.lock) {
            this.lockStage.acquire();
            incrementCounter();
        }
    }

    private void incrementCounter() {
        this.counter.incrementAndGet();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        synchronized (this.lock) {
            this.lockStage.acquireInterruptibly();
            incrementCounter();
        }
    }

    @Override
    public boolean tryLock() {
        synchronized (this.lock) {
            val acquired = this.lockStage.tryAcquire(Duration.ZERO);
            if (acquired) {
                incrementCounter();
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public boolean tryLock(final long time, final TimeUnit unit) throws InterruptedException {
        synchronized (this.lock) {
            val acquired = this.lockStage.tryAcquireInterruptibly(Duration.of(time, unit.toChronoUnit()));
            if (acquired) {
                incrementCounter();
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public void unlock() {
        // May not be the most optimized version, but it serves two goals:
        // 1. Avoid having the counter go negative
        // 2. Avoid re-locking this instance while unlocking the stage
        synchronized (this.lock) {
            val remaining = this.counter.updateAndGet(count -> {
                if (count > 0) {
                    return count - 1;
                } else {
                    throw new IllegalStateException("Already unlocked");
                }
            });
            if (remaining == 0) {
                this.unlockStage.run();
            }
        }
    }

    @Override
    public Condition newCondition() {
        // TODO look at the implementation for ReentrantLock
        throw new UnsupportedOperationException("todo");
    }

}
