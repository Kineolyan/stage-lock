package io.kineolyan.stagelock.internal;

import lombok.RequiredArgsConstructor;
import lombok.val;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.StampedLock;

@RequiredArgsConstructor
public final class ExclusiveLock implements Lock {

    private final StampedLock lock = new StampedLock();
    private long currentStamp;
    private final GateTaker lockStage;
    private final Runnable unlockStage;


    @Override
    public void lock() {
            val stamp = this.lock.writeLock();
            this.currentStamp = stamp;
            this.lockStage.acquire();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
            this.lockStage.acquireInterruptibly();
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
            val stamp = this.lock.tryWriteLock(time, unit);
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
        this.unlockStage.run();
        val stamp = this.currentStamp;
        this.currentStamp = 0;
        this.lock.unlock(stamp);
    }

    @Override
    public Condition newCondition() {
        // TODO look at the implementation for ReentrantLock
        throw new UnsupportedOperationException("todo");
    }

}
