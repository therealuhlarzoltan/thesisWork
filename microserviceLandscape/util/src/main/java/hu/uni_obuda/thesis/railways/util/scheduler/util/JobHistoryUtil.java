package hu.uni_obuda.thesis.railways.util.scheduler.util;

import hu.uni_obuda.thesis.railways.util.collection.Tuple;
import hu.uni_obuda.thesis.railways.util.collection.Tuple.Tuple2;
import hu.uni_obuda.thesis.railways.util.scheduler.model.ScheduledJob;
import hu.uni_obuda.thesis.railways.util.scheduler.model.ScheduledTaskEntry;
import hu.uni_obuda.thesis.railways.util.scheduler.model.JobScheduleDelta;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class JobHistoryUtil {

    private JobHistoryUtil() {}

    public static Flux<JobScheduleDelta> differencePerJob(
            Flux<ScheduledJob> newJobs,
            Map<String, ScheduledTaskEntry> currentJobs
    ) {
        return newJobs.collectMap(ScheduledJob::getName, Function.identity())
                .flatMapMany(desiredByName -> {
                    Set<String> allNames = new HashSet<>(desiredByName.keySet());
                    allNames.addAll(currentJobs.keySet());
                    return Flux.fromIterable(allNames)
                            .map(name -> buildDeltaForJob(name, desiredByName.get(name), currentJobs.get(name)));
                });
    }

    /** Convenience collector. */
    public static Mono<List<JobScheduleDelta>> differenceAsList(
            Flux<ScheduledJob> newJobs,
            Map<String, ScheduledTaskEntry> currentJobs
    ) {
        return differencePerJob(newJobs, currentJobs).collectList();
    }

    private static JobScheduleDelta buildDeltaForJob(
            String name,
            @Nullable ScheduledJob wanted,
            @Nullable ScheduledTaskEntry scheduled
    ) {
        // desired
        Optional<Duration> desiredFixed = Optional.ofNullable(wanted)
                .map(ScheduledJob::getInterval)
                .map(Duration::ofSeconds);
        Set<String> desiredCrons = Optional.ofNullable(wanted)
                .map(ScheduledJob::getCrons)
                .map(HashSet::new)
                .orElseGet(HashSet::new);

        // current
        Optional<Duration> currentFixed = Optional.ofNullable(scheduled)
                .flatMap(ScheduledTaskEntry::getFixedRateInterval);
        Set<String> currentCrons = Optional.ofNullable(scheduled)
                .map(ScheduledTaskEntry::getCronExpressions)
                .map(HashSet::new)
                .orElseGet(HashSet::new);

        // fixed: add/remove (+ interval change => remove+add)
        boolean wantFixed = desiredFixed.isPresent();
        boolean hasFixed  = currentFixed.isPresent();
        boolean intervalDiff = wantFixed && hasFixed && !desiredFixed.get().equals(currentFixed.get());

        Tuple2<String, Duration> fixedToAdd = null;
        List<String> fixedToRemove = new ArrayList<>(1);

        if (wantFixed && (!hasFixed || intervalDiff)) {
            fixedToAdd = Tuple.of(name, desiredFixed.get());
        }
        if ((!wantFixed && hasFixed) || intervalDiff) {
            fixedToRemove.add(name);
        }

        // cron: adds/removes
        Set<String> cronAddsSet = desiredCrons.stream()
                .filter(expr -> !currentCrons.contains(expr))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Tuple2<String, Set<String>> cronToAdd =
                cronAddsSet.isEmpty() ? null : Tuple.of(name, cronAddsSet);

        List<Tuple2<String, String>> cronToRemove = currentCrons.stream()
                .filter(expr -> !desiredCrons.contains(expr))
                .map(expr -> Tuple.of(name, expr))
                .collect(Collectors.toCollection(ArrayList::new));

        return new JobScheduleDelta(
                fixedToAdd,
                cronToAdd,
                Collections.unmodifiableList(fixedToRemove),
                Collections.unmodifiableList(cronToRemove)
        );
    }
}
