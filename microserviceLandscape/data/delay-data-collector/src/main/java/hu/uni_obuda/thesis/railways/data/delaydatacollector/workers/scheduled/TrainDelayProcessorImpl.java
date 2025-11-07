package hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.scheduled;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.TrainStatusCache;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.domain.TrainRouteEntity;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.domain.TrainStationEntity;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.repository.domain.TrainRouteRepository;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.repository.domain.TrainStationRepository;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.service.DelayFetcherService;
import hu.uni_obuda.thesis.railways.util.scheduler.annotation.ScheduledJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Profile("production")
@Component
public class TrainDelayProcessorImpl implements TrainDelayProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(TrainDelayProcessorImpl.class);
    private static final Map<Character, Character> stationCodeMapping = Map.of(
            'ő', 'õ',
            'ű', 'û'
    );

    private  final Scheduler scheduler;
    private final TrainRouteRepository trainRouteRepository;
    private final DelayFetcherService delayFetcherService;
    private final TrainStatusCache trainStatusCache;
    private final TrainStationRepository trainStationRepository;

    @Autowired
    public TrainDelayProcessorImpl(@Qualifier("trainDelayProcessorScheduler") Scheduler scheduler, TrainRouteRepository trainRouteRepository,
                                   DelayFetcherService delayFetcherService, TrainStatusCache trainStatusCache, TrainStationRepository trainStationRepository) {
        this.scheduler = scheduler;
        this.trainRouteRepository = trainRouteRepository;
        this.delayFetcherService = delayFetcherService;
        this.trainStatusCache = trainStatusCache;
        this.trainStationRepository = trainStationRepository;
    }

    @ScheduledJob("dataFetch")
    @Override
    public void processTrainRoutes() {
        LOG.info("Data fetch started...");
        LocalDateTime now = LocalDateTime.now();


        trainRouteRepository.findAll()
                .flatMap(trainRoute -> processTrainIfIncomplete(trainRoute, resolveOperationalDate(now)))
                .subscribeOn(scheduler)
                .subscribe();

    }

    @Override
    public void processTrainRoute(String trainNumber) {
        LOG.info("Data fetch started for single train with train number: {}", trainNumber);
        LocalDateTime now = LocalDateTime.now();

        trainRouteRepository.findById(trainNumber)
                .flatMap(trainRoute -> processTrainIfIncomplete(trainRoute, resolveOperationalDate(now)))
                .subscribeOn(scheduler)
                .subscribe();
    }

    private Mono<Void> processTrainIfIncomplete(TrainRouteEntity trainRoute, LocalDate date) {
        LOG.info("Fetching delay for train number {}", trainRoute.getTrainNumber());
        return trainStatusCache.isComplete(trainRoute.getTrainNumber(), date)
                .flatMap(complete -> {
                    if (complete) {
                        LOG.info("Data for train number {} is already present for today", trainRoute.getTrainNumber());
                        return Mono.empty();
                    } else {
                        LOG.info("Calling fetcher service for train number {}", trainRoute.getTrainNumber());
                        Mono<TrainStationEntity> startStation = trainStationRepository.findByStationCode(adjustStationCodeFormat(trainRoute.getFrom()));
                        Mono<TrainStationEntity> endStation = trainStationRepository.findByStationCode(adjustStationCodeFormat(trainRoute.getTo()));
                        Double startStationLatitude = startStation.map(TrainStationEntity::getLatitude).block();
                        Double startStationLongitude = startStation.map(TrainStationEntity::getLongitude).block();
                        Double endStationLatitude = endStation.map(TrainStationEntity::getLatitude).block();
                        Double endStationLongitude = endStation.map(TrainStationEntity::getLongitude).block();
                        if (startStationLatitude == null ||startStationLongitude == null) {
                            return Mono.empty();
                        }
                        if (endStationLatitude == null || endStationLongitude == null) {
                            return Mono.empty();
                        }

                        return Mono.fromRunnable(() ->
                                delayFetcherService.fetchDelay(
                                        trainRoute.getTrainNumber(),
                                        trainRoute.getFrom(),
                                        startStationLatitude,
                                        startStationLongitude,
                                        trainRoute.getTo(),
                                        endStationLatitude,
                                        endStationLongitude,
                                        date
                                )
                        );
                    }
                });
    }

    private LocalDate resolveOperationalDate(LocalDateTime date) {
        if (date.getHour() < 3)
            return date.toLocalDate().minusDays(1);
        return date.toLocalDate();
    }

    private String adjustStationCodeFormat(@NonNull String stationCode) {
        for (int i = 0; i < stationCode.length(); i++) {
            if (stationCodeMapping.containsKey(stationCode.charAt(i))) {
                stationCode = stationCode.replace(stationCode.charAt(i), stationCodeMapping.get(stationCode.charAt(i)));
            }
        }
        return stationCode;
    }
}
