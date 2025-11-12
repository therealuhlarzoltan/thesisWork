package hu.uni_obuda.thesis.railways.util.scheduler.event;

import hu.uni_obuda.thesis.railways.util.scheduler.entity.JobEntity;

import java.time.Clock;

public final class ScheduledJobAddedEvent extends ScheduledJobEvent {

   public ScheduledJobAddedEvent(Object source, JobEntity jobEntity) {
        super(source, jobEntity);
    }

    public ScheduledJobAddedEvent(Object source, JobEntity jobEntity, Clock clock) {
        super(source, jobEntity, clock);
    }
}
