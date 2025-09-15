package hu.uni_obuda.thesis.railways.data.delaydatacollector.service;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.messaging.senders.MessageSender;
import hu.uni_obuda.thesis.railways.data.event.CrudEvent;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfoRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.LocalDate;

@Service
public class DelayFetcherServiceImpl implements DelayFetcherService {

    private static final Logger LOG = LoggerFactory.getLogger(DelayFetcherServiceImpl.class);

    private final Scheduler scheduler;
    private final MessageSender messageSender;

    @Autowired
    public DelayFetcherServiceImpl(@Qualifier("messageSenderScheduler") Scheduler scheduler, MessageSender messageSender) {
        this.scheduler = scheduler;
        this.messageSender = messageSender;
    }

    @Override
    public void fetchDelay(String trainNumber, String from, double fromLatitude, double fromLongitude, String to, double toLatitude, double toLongitude, LocalDate date) {
        LOG.info("Fetching delay for train number {}", trainNumber);
        Mono.fromRunnable(() -> {
            DelayInfoRequest delayInfoRequest = new DelayInfoRequest(trainNumber, from, fromLatitude, fromLongitude, to, toLatitude, toLongitude, date);
            CrudEvent<String, DelayInfoRequest> crudEvent = new CrudEvent<>(CrudEvent.Type.GET, trainNumber, delayInfoRequest);
            messageSender.sendMessage("railDataRequests-out-0", crudEvent);
        }).subscribeOn(scheduler).subscribe();
    }

    @Override
    public void fetchDelay(String correlationId, String trainNumber, String from, double fromLatitude, double fromLongitude, String to, double toLatitude, double toLongitude, LocalDate date) {
        LOG.info("Fetching delay for train number {}", trainNumber);
        Mono.fromRunnable(() -> {
            DelayInfoRequest delayInfoRequest = new DelayInfoRequest(trainNumber, from, fromLatitude, fromLongitude, to, toLatitude, toLongitude, date);
            CrudEvent<String, DelayInfoRequest> crudEvent = new CrudEvent<>(CrudEvent.Type.GET, trainNumber, delayInfoRequest);
            messageSender.sendMessage("railDataRequests-out-0", correlationId, crudEvent);
        }).subscribeOn(scheduler).subscribe();
    }
}
