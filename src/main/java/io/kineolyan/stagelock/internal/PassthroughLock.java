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
    private final Object signal = new Object();


    @Override
    public void lock() {
        while (true) {
            val optimisticStart = incrementCounter();
            if (optimisticStart < 0) {
                waitSignal(null);
            } else {
                break;
            }
        }
        this.lockStage.acquire();
    }

    private int incrementCounter() {
        return this.counter.updateAndGet(count -> {
            if (count < 0) {
                return count;
            } else {
                return count + 1;
            }
        });
    }

    private void waitSignal(final Duration timeout) {
        synchronized (this.signal) {
            try {
                if (timeout == null) {
                    this.signal.wait();
                } else {
                    this.signal.wait(timeout.toMillisPart(), timeout.toNanosPart());
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void waitSignalInterruptibly(final Duration timeout) throws InterruptedException {
        synchronized (this.signal) {
            if (timeout == null) {
                this.signal.wait();
            } else {
                this.signal.wait(timeout.toMillisPart(), timeout.toNanosPart());
            }
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        while (true) {
            val optimisticStart = incrementCounter();
            if (optimisticStart < 0) {
                waitSignalInterruptibly(null);
            } else {
                break;
            }
        }
        this.lockStage.acquire();
    }

    @Override
    public boolean tryLock() {
        while (true) {
            val optimisticStart = incrementCounter();
            if (optimisticStart < 0) {
                waitSignal(null);
            } else {
                break;
            }
        }
        val acquired = this.lockStage.tryAcquire(Duration.ZERO);
        if (acquired) {
            return true;
        } else {
            // Still not good, we can go from a false 1 -> 0, but we don't need to release the gate
            this.counter.decrementAndGet();
            return false;
        }
    }

    @Override
    public boolean tryLock(final long time, final TimeUnit unit) throws InterruptedException {
        while (true) {
            val optimisticStart = incrementCounter();
            if (optimisticStart < 0) {
                waitSignal(Duration.of(time, unit.toChronoUnit()));
            } else {
                break;
            }
        }
        val acquired = this.lockStage.tryAcquireInterruptibly(Duration.of(time, unit.toChronoUnit()));
        if (acquired) {
            return true;
        } else {
            // Still not good, we can go from a false 1 -> 0, but we don't need to release the gate
            this.counter.decrementAndGet();
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
            assert reset : "Failed to reset the lock";
            synchronized (this.signal) {
                this.signal.notifyAll();
            }
        }
    }

    @Override
    public Condition newCondition() {
        // TODO look at the implementation for ReentrantLock
        throw new UnsupportedOperationException("todo");
    }

}
