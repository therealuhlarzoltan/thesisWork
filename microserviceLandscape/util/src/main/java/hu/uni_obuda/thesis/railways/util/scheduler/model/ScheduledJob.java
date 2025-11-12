package hu.uni_obuda.thesis.railways.util.scheduler.model;

import hu.uni_obuda.thesis.railways.util.scheduler.entity.CronEntity;
import hu.uni_obuda.thesis.railways.util.scheduler.entity.IntervalEntity;
import hu.uni_obuda.thesis.railways.util.scheduler.entity.JobEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ScheduledJob {

    private final JobEntity job;
    private final IntervalEntity interval;
    private final Collection<CronEntity> crons;

    public Integer getId() {
        return job.getId();
    }

    public String getName() {
        return job.getName();
    }

    public Set<String> getCrons() {
        return crons.stream().map(CronEntity::getChronExpression).collect(Collectors.toUnmodifiableSet());
    }

    public @Nullable Long getInterval() {
        return (interval == null) ? null : interval.getIntervalInMillis();
    }

    @Override
    public String toString() {
        return job.getName();
    }
}
