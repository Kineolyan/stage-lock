package io.kineolyan.stagelock;

import io.kineolyan.stagelock.internal.ExclusiveLock;
import io.kineolyan.stagelock.internal.PassthroughLock;
import io.kineolyan.stagelock.internal.StageGate;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class StagedLock<StageT> {

    // Kept mostly for inspection
    private final StageGate<StageT> gate;
    private final Map<StageT, Lock> locks;

    public static <T> StagedLock<T> create(final @NonNull Map<T, LockStrategy> definition) {
        if (definition.size() <= 1) {
            throw new IllegalArgumentException("Lock definition must at least contain two stages.");
        }
        final UnaryOperator<Map<T, Lock>> mapFinalizer;
        if (definition.keySet().stream().allMatch(k -> k.getClass().isInstance(Enum.class))) {
//            mapFinalizer = m -> new EnumMap<Enum<StageT>, Lock>(m);
            throw new UnsupportedOperationException("todo");
        } else {
            mapFinalizer = Map::copyOf;
        }
        val gate = StageGate.<T>create();
        return new StagedLock<>(
                gate,
                definition.entrySet().stream().collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> {
                                    val stage = entry.getKey();
                                    return switch (entry.getValue()) {
                                        case PASSTHROUGH -> new PassthroughLock(
                                                gate.bindToStage(stage));
                                        case EXCLUSIVE -> new ExclusiveLock(
                                                gate.bindToStage(stage));
                                    };
                                }),
                        mapFinalizer
                )));
    }

    public Lock getLock(final StageT stage) {
        return Objects.requireNonNull(
                this.locks.get(stage),
                () -> "No lock for " + stage);
    }

    public enum LockStrategy {
        PASSTHROUGH,
        EXCLUSIVE
    }

}
