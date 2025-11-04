package hu.uni_obuda.thesis.railways.util.scheduler.event;

import hu.uni_obuda.thesis.railways.util.scheduler.entity.JobEntity;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Clock;

@Getter
public sealed abstract class ScheduledJobEvent extends ApplicationEvent permits ScheduledJobAddedEvent, ScheduledJobModifiedEvent, ScheduledJobRemovedEvent{

    protected final JobEntity jobEntity;

    protected ScheduledJobEvent(Object source, JobEntity jobEntity) {
        super(source);
        this.jobEntity = jobEntity;
    }

    protected ScheduledJobEvent(Object source, JobEntity jobEntity, Clock clock) {
        super(source, clock);
        this.jobEntity = jobEntity;
    }
}
