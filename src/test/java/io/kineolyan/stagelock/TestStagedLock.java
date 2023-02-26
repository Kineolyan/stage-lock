package io.kineolyan.stagelock;

import io.kineolyan.stagelock.StagedLock.LockStrategy;
import lombok.val;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class TestStagedLock {

    @Test
    void testCreation() {
        val lock = StagedLock.create(Map.of("a", LockStrategy.PASSTHROUGH, "b", LockStrategy.PASSTHROUGH));
        val aLock = lock.getLock("a");
        val bLock = lock.getLock("b");
        val bLocked = bLock.tryLock();
        assertThat(bLocked).as("b locked").isTrue();
        assertThat(aLock.tryLock()).as("locking a").isFalse();
        bLock.unlock();
        assertThat(aLock.tryLock()).as("a locked after b").isTrue();
    }

    @Test
    void testPassthroughLock() {
        val lock = StagedLock.create(Map.of("a", LockStrategy.PASSTHROUGH, "b", LockStrategy.PASSTHROUGH));
        val aLock = lock.getLock("a");
        val bLock = lock.getLock("b");

        SoftAssertions.assertSoftly(assertions -> {
            IntStream.range(0, 3).forEach(i -> assertions.assertThat(aLock.tryLock()).as("Locking a for the %d nth time", i + 1).isTrue());
        });
        assertThat(bLock.tryLock()).as("locking b").isFalse();

        IntStream.range(0, 2).forEach(i -> aLock.unlock());
        assertThat(bLock.tryLock()).as("b lock still unavailable").isFalse();

        aLock.unlock();
        assertThat(bLock.tryLock()).as("b lock finally available").isTrue();
    }

    @Test
    void testExclusiveLock() {
        val lock = StagedLock.create(Map.of("a", LockStrategy.EXCLUSIVE, "b", LockStrategy.EXCLUSIVE));
        val aLock = lock.getLock("a");
        val bLock = lock.getLock("b");

        val aLocked = aLock.tryLock();
        assertThat(aLocked).as("a locked").isTrue();
        assertThat(bLock.tryLock()).as("locking b").isFalse();
        assertThat(aLock.tryLock()).as("relocking a").isFalse();

        aLock.unlock();
        assertThat(bLock.tryLock()).as("b lock finally available").isTrue();
        assertThat(aLock.tryLock()).as("locking a with b taken").isFalse();
    }

}