package hu.uni_obuda.thesis.railways.data.delaydatacollector.component.listener;

import hu.uni_obuda.thesis.railways.util.scheduler.ReactiveCustomScheduler;
import hu.uni_obuda.thesis.railways.util.scheduler.event.ScheduledJobEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class ScheduledJobEventListener {

    private final ObjectProvider<ReactiveCustomScheduler> schedulerProvider;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onScheduledJob(ScheduledJobEvent<?> event) {
        log.info("Detected a Scheduled Job Event!");

        ReactiveCustomScheduler scheduler = schedulerProvider.getIfAvailable();
        if (scheduler == null) {
            log.warn("Scheduler not available; skipping scheduling!");
            return;
        }

        scheduler.startSchedulingAfterEvent(event);
    }
}
