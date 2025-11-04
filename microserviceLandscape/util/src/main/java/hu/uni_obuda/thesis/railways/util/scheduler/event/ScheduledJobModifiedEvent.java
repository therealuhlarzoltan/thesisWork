package hu.uni_obuda.thesis.railways.util.scheduler.event;

import hu.uni_obuda.thesis.railways.util.scheduler.entity.JobEntity;

import java.time.Clock;

public final class ScheduledJobModifiedEvent extends ScheduledJobEvent {

    public ScheduledJobModifiedEvent(Object source, JobEntity jobEntity) {
        super(source, jobEntity);
    }

    public ScheduledJobModifiedEvent(Object source, JobEntity jobEntity, Clock clock) {
        super(source, jobEntity, clock);
    }
}
