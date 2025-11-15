package hu.uni_obuda.thesis.railways.util.scheduler.event;

import hu.uni_obuda.thesis.railways.util.scheduler.entity.JobEntity;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Clock;

@Getter
public sealed abstract class ScheduledJobEvent<E extends JobEntity> extends ApplicationEvent permits ScheduledJobAddedEvent, ScheduledJobModifiedEvent, ScheduledJobRemovedEvent{

    protected final E jobEntity;

    protected ScheduledJobEvent(Object source, E jobEntity) {
        super(source);
        this.jobEntity = jobEntity;
    }

    protected ScheduledJobEvent(Object source, E jobEntity, Clock clock) {
        super(source, clock);
        this.jobEntity = jobEntity;
    }
}
