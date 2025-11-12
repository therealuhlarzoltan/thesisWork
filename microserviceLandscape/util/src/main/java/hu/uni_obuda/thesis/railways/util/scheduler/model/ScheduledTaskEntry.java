package hu.uni_obuda.thesis.railways.util.scheduler.model;

import hu.uni_obuda.thesis.railways.util.collection.Tuple;
import hu.uni_obuda.thesis.railways.util.collection.Tuple.Tuple2;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
public class ScheduledTaskEntry {

    @Getter
    private final String name;
    private final AtomicReference<Tuple2<Duration, ScheduledFuture<?>>> fixedRateTask = new AtomicReference<>();
    private final ConcurrentMap<String, ScheduledFuture<?>> cronTasks = new ConcurrentHashMap<>();

    public void setFixedRateTask(Duration interval, ScheduledFuture<?> fixedRateTask) {
        this.fixedRateTask.set(Tuple.of(interval, fixedRateTask));
    }

    public Optional<Duration> getFixedRateInterval() {
        return Optional.ofNullable(fixedRateTask.get()).map(Tuple2::first);
    }

    public Optional<ScheduledFuture<?>> getFixedRateTask() {
        return Optional.ofNullable(fixedRateTask.get()).map(Tuple2::second);
    }

    public void addCronTask(String cronExpression, ScheduledFuture<?> cronTask) {
        cronTasks.put(cronExpression, cronTask);
    }

    public Optional<ScheduledFuture<?>> getCronTask(String cronExpression) {
        return Optional.ofNullable(cronTasks.get(cronExpression));
    }

    public void removeCronTask(String cronExpression) {
        cronTasks.remove(cronExpression);
    }

    public Set<String> getCronExpressions() {
        return cronTasks.keySet();
    }
}
