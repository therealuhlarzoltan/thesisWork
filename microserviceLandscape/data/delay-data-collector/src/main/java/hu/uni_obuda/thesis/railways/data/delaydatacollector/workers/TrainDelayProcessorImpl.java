package hu.uni_obuda.thesis.railways.data.delaydatacollector.workers;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.TrainStatusCache;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.TrainRouteEntity;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.repository.TrainRouteRepository;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.service.DelayFetcherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.LocalDate;

@Component
public class TrainDelayProcessorImpl implements TrainDelayProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(TrainDelayProcessorImpl.class);
    private static final long PROCESSING_INTERVAL_IN_MILLIS = 3_600_000; // every hour

    private  final Scheduler scheduler;
    private final TrainRouteRepository trainRouteRepository;
    private final DelayFetcherService delayFetcherService;
    private final TrainStatusCache trainStatusCache;;

    @Autowired
    public TrainDelayProcessorImpl(@Qualifier("trainDelayProcessorScheduler") Scheduler scheduler, TrainRouteRepository trainRouteRepository,
                                   DelayFetcherService delayFetcherService, TrainStatusCache trainStatusCache) {
        this.scheduler = scheduler;
        this.trainRouteRepository = trainRouteRepository;
        this.delayFetcherService = delayFetcherService;
        this.trainStatusCache = trainStatusCache;
    }

    @Scheduled(fixedDelay = PROCESSING_INTERVAL_IN_MILLIS)
    @Override
    public void processTrainRoutes() {
        LOG.info("Data fetch started...");
        LocalDate today = LocalDate.now();

        trainRouteRepository.findAll()
                .flatMap(trainRoute -> processTrainIfIncomplete(trainRoute, today))
                .subscribeOn(scheduler)
                .subscribe();
    }

    private Mono<Void> processTrainIfIncomplete(TrainRouteEntity trainRoute, LocalDate date) {
        LOG.info("Fetching date for train number {}", trainRoute.getTrainNumber());
        return trainStatusCache.isComplete(trainRoute.getTrainNumber(), date)
                .flatMap(complete -> {
                    if (complete) {
                        LOG.info("Data for train number {} is already present for today", trainRoute.getTrainNumber());
                        return Mono.empty();
                    } else {
                        LOG.info("Calling fetcher service for train number {}", trainRoute.getTrainNumber());
                        return Mono.fromRunnable(() ->
                                delayFetcherService.fetchDelay(
                                        trainRoute.getTrainNumber(),
                                        trainRoute.getFrom(),
                                        trainRoute.getTo(),
                                        LocalDate.now()
                                )
                        );
                    }
                });
    }
}
