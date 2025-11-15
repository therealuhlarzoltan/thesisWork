package hu.uni_obuda.thesis.railways.util.scheduler.model;

import hu.uni_obuda.thesis.railways.util.scheduler.entity.CronEntity;
import hu.uni_obuda.thesis.railways.util.scheduler.entity.IntervalEntity;
import hu.uni_obuda.thesis.railways.util.scheduler.entity.JobEntity;
import org.springframework.lang.Nullable;


import java.util.*;
import java.util.stream.Collectors;

public class ScheduledJob {

    private final JobEntity job;
    private final @Nullable IntervalEntity interval;
    private final List<CronEntity> crons;

    public ScheduledJob(JobEntity job,
                        @Nullable IntervalEntity interval,
                        List<? extends CronEntity> crons) {
        this.job = job;
        this.interval = interval;
        this.crons = new ArrayList<>(crons);
    }

    public Integer getId() {
        return job.getId();
    }

    public String getName() {
        return job.getName();
    }

    public Set<String> getCrons() {
        return crons.stream().map(CronEntity::getCronExpression).collect(Collectors.toUnmodifiableSet());
    }

    public @Nullable Long getInterval() {
        return (interval == null) ? null : interval.getIntervalInMillis();
    }

    @Override
    public String toString() {
        return job.getName();
    }
}
