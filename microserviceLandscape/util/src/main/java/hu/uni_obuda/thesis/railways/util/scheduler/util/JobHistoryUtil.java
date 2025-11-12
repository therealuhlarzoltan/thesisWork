package hu.uni_obuda.thesis.railways.util.scheduler.util;

import hu.uni_obuda.thesis.railways.util.scheduler.model.JobScheduleDelta;
import hu.uni_obuda.thesis.railways.util.scheduler.model.ScheduledJob;
import hu.uni_obuda.thesis.railways.util.scheduler.model.ScheduledTaskEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
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

    public static Mono<List<JobScheduleDelta>> differenceAsList(
            Flux<ScheduledJob> newJobs,
            Map<String, ScheduledTaskEntry> currentJobs
    ) {
        log.info("Collecting scheduled job deltas...");
        return differencePerJob(newJobs, currentJobs).collectList();
    }

    private static JobScheduleDelta buildDeltaForJob(
            String name,
            @Nullable ScheduledJob wanted,
            @Nullable ScheduledTaskEntry scheduled
    ) {

        log.info("Building delta for job {}", name);
        Optional<Duration> desiredFixed =
                Optional.ofNullable(wanted)
                        .map(ScheduledJob::getInterval)
                        .map(ms -> ms == null ? null : Duration.ofMillis(ms));
        Set<String> desiredCrons =
                Optional.ofNullable(wanted)
                        .map(ScheduledJob::getCrons)
                        .map(HashSet::new)
                        .orElseGet(HashSet::new);

        Optional<Duration> currentFixed = Optional
                .ofNullable(scheduled)
                .flatMap(ScheduledTaskEntry::getFixedRateInterval);
        Set<String> currentCrons = Optional
                .ofNullable(scheduled)
                .map(ScheduledTaskEntry::getCronExpressions)
                .map(HashSet::new)
                .orElseGet(HashSet::new);

        boolean wantFixed = desiredFixed.isPresent();
        boolean hasFixed = currentFixed.isPresent();
        boolean intervalDiff  = wantFixed && hasFixed && !desiredFixed.get().equals(currentFixed.get());

        Duration fixedToAdd = (wantFixed && (!hasFixed || intervalDiff)) ? desiredFixed.get() : null;


        Duration fixedToRemove = ((!wantFixed && hasFixed) || intervalDiff) ? currentFixed.orElse(Duration.ZERO) : null;


        Set<String> cronsToAdd = desiredCrons.stream()
                .filter(expr -> !currentCrons.contains(expr))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> cronsToRemove = currentCrons.stream()
                .filter(expr -> !desiredCrons.contains(expr))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return new JobScheduleDelta(
                name,
                fixedToAdd,
                Collections.unmodifiableSet(cronsToAdd),
                fixedToRemove,
                Collections.unmodifiableSet(cronsToRemove)
        );
    }
}