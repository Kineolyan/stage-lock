package io.kineolyan.stagelock.internal;

import java.time.Duration;
import java.util.Objects;

public class StageGate<StageT> {

    private final Object lock = new Object();
    private StageT activeStage = null;

    public static <T> StageGate<T> create() {
        return new StageGate<>();
    }

    public boolean acquire(final StageT stage, final Duration timeout) throws InterruptedException {
        synchronized (this.lock) {
            while (true) {
                if (Objects.equals(this.activeStage, stage)) {
                    return true;
                } else if (this.activeStage == null) {
                    this.activeStage = stage;
                    return true;
                } else {
                    if (timeout == null) {
                        this.lock.wait();
                    } else if (Objects.equals(timeout, Duration.ZERO)) {
                        return false;
                    } else {
                        this.lock.wait(timeout.toMillisPart(), timeout.toNanosPart());
                    }
                }
            }
        }
    }

    public void release(final StageT stage) {
        synchronized (this.lock) {
            if (Objects.equals(this.activeStage, stage)) {
                this.activeStage = null;
                this.lock.notifyAll();
            } else {
                throw new IllegalStateException(String.format("Stage %s not locked (current=%s)", stage, this.activeStage));
            }
        }
    }

    public GateTaker bindToStage(final StageT stage) {
        return new GateTaker() {
            @Override
            public boolean tryAcquireInterruptibly(Duration timeout) throws InterruptedException {
                return StageGate.this.acquire(stage, timeout);
            }

            @Override
            public void release() {
                StageGate.this.release(stage);

            }
        };
    }

}
