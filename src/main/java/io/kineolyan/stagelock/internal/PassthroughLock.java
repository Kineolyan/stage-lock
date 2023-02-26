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


    @Override
    public void lock() {
        this.lockStage.acquire();
        incrementCounter();
    }

    private void incrementCounter() {
        this.counter.incrementAndGet();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        this.lockStage.acquireInterruptibly();
        incrementCounter();
    }

    @Override
    public boolean tryLock() {
        val acquired = this.lockStage.tryAcquire(Duration.ZERO);
        if (acquired) {
            incrementCounter();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean tryLock(final long time, final TimeUnit unit) throws InterruptedException {
        val acquired = this.lockStage.tryAcquireInterruptibly(Duration.of(time, unit.toChronoUnit()));
        if (acquired) {
            incrementCounter();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void unlock() {
        // May not be the most optimized version, but it serves two goals:
        // 1. Avoid having the counter go negative
        // 2. Avoid relocking this instance while unlocking the stage
        // FIXME not working: we must wait somehow while the stage is being unlocked
        val remaining = this.counter.updateAndGet(count -> {
            if (count > 1) {
                return count - 1;
            } else if (count == 0) {
                return -1;
            } else {
                throw new IllegalStateException("Already unlocked");
            }
        });
        if (remaining == -1) {
            this.unlockStage.run();
            val reset = this.counter.compareAndSet(-1, 0);
            assert reset: "Failed to reset the lock";
        }
    }

    @Override
    public Condition newCondition() {
        // TODO look at the implementation for ReentrantLock
        throw new UnsupportedOperationException("todo");
    }

}
