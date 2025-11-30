package hu.uni_obuda.thesis.railways.data.delaydatacollector.component.listener;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.scheduling.ScheduledJobEntity;
import hu.uni_obuda.thesis.railways.util.scheduler.ReactiveCustomScheduler;
import hu.uni_obuda.thesis.railways.util.scheduler.repository.ReactiveCompositeJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ApplicationReadyEventListener {

    private final ObjectProvider<ReactiveCustomScheduler> schedulerProvider;
    private final ObjectProvider<ReactiveCompositeJobRepository<ScheduledJobEntity>> repositoryProvider;

    @Async
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        log.info("Detected an ApplicationReady Event!");
        ReactiveCustomScheduler scheduler = schedulerProvider.getIfAvailable();
        ReactiveCompositeJobRepository<ScheduledJobEntity> repository = repositoryProvider.getIfAvailable();

        if (scheduler == null || repository == null) {
            log.warn("Scheduler or job repository not available; skipping scheduling!");
            return;
        }

        repository.init();
        scheduler.startSchedulingAfterEvent(event);
    }
}