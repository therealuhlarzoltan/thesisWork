package hu.uni_obuda.thesis.railways.data.delaydatacollector.component.listener;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.scheduling.ScheduledJobEntity;
import hu.uni_obuda.thesis.railways.util.scheduler.ReactiveCustomScheduler;
import hu.uni_obuda.thesis.railways.util.scheduler.repository.ReactiveCompositeJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ApplicationReadyEventListener {

    private final ReactiveCustomScheduler scheduler;
    private final ReactiveCompositeJobRepository<ScheduledJobEntity> jobRepository;

    @Async
    @EventListener
    public void onContextRefreshed(ApplicationReadyEvent event) {
        log.info("Detected an ApplicationReady Event!");
        jobRepository.init();
        scheduler.startSchedulingAfterEvent(event);
    }
}