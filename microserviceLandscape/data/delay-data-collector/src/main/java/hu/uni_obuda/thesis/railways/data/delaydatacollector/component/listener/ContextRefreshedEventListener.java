package hu.uni_obuda.thesis.railways.data.delaydatacollector.component.listener;

import hu.uni_obuda.thesis.railways.util.scheduler.ReactiveCustomScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ContextRefreshedEventListener {

    private final ReactiveCustomScheduler scheduler;

    @Async
    @EventListener
    public void onContextRefreshed(ContextRefreshedEvent event) {
        log.info("Detected a ContextRefreshed Event!");
        scheduler.startSchedulingAfterEvent(event);
    }
}