package hu.uni_obuda.thesis.railways.util.scheduler.event;

import hu.uni_obuda.thesis.railways.util.scheduler.entity.JobEntity;

import java.time.Clock;

public final class ScheduledJobAddedEvent<E extends JobEntity> extends ScheduledJobEvent<E> {

   public ScheduledJobAddedEvent(Object source, E jobEntity) {
        super(source, jobEntity);
    }

    public ScheduledJobAddedEvent(Object source, E jobEntity, Clock clock) {
        super(source, jobEntity, clock);
    }
}
