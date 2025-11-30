package hu.uni_obuda.thesis.railways.data.delaydatacollector.service.data.impl;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.service.data.DelayFetcherService;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.messaging.sender.MessageSender;
import hu.uni_obuda.thesis.railways.data.event.CrudEvent;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfoRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.LocalDate;

@Profile("data-source-elvira")
@Service
@Slf4j
public class ElviraDelayFetcherService implements DelayFetcherService {

    private final Scheduler scheduler;
    private final MessageSender messageSender;

    @Autowired
    public ElviraDelayFetcherService(@Qualifier("messageSenderScheduler") Scheduler scheduler, MessageSender messageSender) {
        this.scheduler = scheduler;
        this.messageSender = messageSender;
    }

    @Override
    public void fetchDelay(String trainNumber, String from, double fromLatitude, double fromLongitude, String to, double toLatitude, double toLongitude, LocalDate date) {
        log.info("Fetching delay for train number {}", trainNumber);
        Mono.fromRunnable(() -> {
            DelayInfoRequest delayInfoRequest = new DelayInfoRequest(trainNumber, from, -1, -1, to, -1, -1, date);
            CrudEvent<String, DelayInfoRequest> crudEvent = new CrudEvent<>(CrudEvent.Type.GET, trainNumber, delayInfoRequest);
            messageSender.sendMessage("railDataRequests-out-0", crudEvent);
        }).subscribeOn(scheduler).subscribe();
    }

    @Override
    public void fetchDelay(String correlationId, String trainNumber, String from, double fromLatitude, double fromLongitude, String to, double toLatitude, double toLongitude, LocalDate date) {
        log.info("Fetching delay for train number {}", trainNumber);
        Mono.fromRunnable(() -> {
            DelayInfoRequest delayInfoRequest = new DelayInfoRequest(trainNumber, from, -1, -1, to, -1, -1, date);
            CrudEvent<String, DelayInfoRequest> crudEvent = new CrudEvent<>(CrudEvent.Type.GET, trainNumber, delayInfoRequest);
            messageSender.sendMessage("railDataRequests-out-0", correlationId, crudEvent);
        }).subscribeOn(scheduler).subscribe();
    }
}
