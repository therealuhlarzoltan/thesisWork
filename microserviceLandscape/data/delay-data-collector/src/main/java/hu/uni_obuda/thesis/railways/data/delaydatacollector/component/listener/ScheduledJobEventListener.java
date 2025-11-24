package hu.uni_obuda.thesis.railways.data.delaydatacollector.component.listener;

import hu.uni_obuda.thesis.railways.util.scheduler.ReactiveCustomScheduler;
import hu.uni_obuda.thesis.railways.util.scheduler.event.ScheduledJobEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@ConditionalOnBean(ReactiveCustomScheduler.class)
@Component
@Slf4j
@RequiredArgsConstructor
public class ScheduledJobEventListener {

    private final ReactiveCustomScheduler scheduler;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onScheduledJob(ScheduledJobEvent<?> event) {
        log.info("Detected a Scheduled Job Event!");
        scheduler.startSchedulingAfterEvent(event);
    }
}
