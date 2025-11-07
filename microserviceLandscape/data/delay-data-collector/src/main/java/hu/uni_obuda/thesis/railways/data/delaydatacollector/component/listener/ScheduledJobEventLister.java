package hu.uni_obuda.thesis.railways.data.delaydatacollector.component.listener;

import hu.uni_obuda.thesis.railways.util.scheduler.ReactiveCustomScheduler;
import hu.uni_obuda.thesis.railways.util.scheduler.event.ScheduledJobEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ScheduledJobEventLister {

    private final ReactiveCustomScheduler scheduler;

    @Async
    @EventListener
    public void onScheduledJob(ScheduledJobEvent event) {
        log.info("Detected a Scheduled Job Event!");
        scheduler.startSchedulingAfterEvent(event);
    }
}
