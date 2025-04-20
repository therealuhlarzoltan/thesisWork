package hu.uni_obuda.thesis.railways.data.delaydatacollector.service;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.DelayInfoCache;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.TrainStatusCache;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.DelayEntity;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.TrainStationEntity;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.mapper.DelayMapper;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.repository.DelayRepository;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.repository.TrainStationRepository;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.util.StringUtils;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.LocalDateTime;

@Service
public class DelayServiceImpl implements DelayService {

    private static final Logger LOG = LoggerFactory.getLogger(DelayServiceImpl.class);

    private final DelayRepository delayRepository;
    private final TrainStationRepository stationRepository;
    private final WeatherService weatherService;
    private final DelayInfoCache delayInfoCache;
    private final TrainStatusCache trainStatusCache;
    private final DelayMapper mapper;
    private final Scheduler scheduler;

    @Autowired
    public DelayServiceImpl(DelayRepository delayRepository, TrainStationRepository stationRepository, WeatherService weatherService, DelayInfoCache delayInfoCache,
                            DelayMapper delayMapper, TrainStatusCache trainStatusCache,
                            @Qualifier("messageProcessingScheduler") Scheduler scheduler) {
        this.delayRepository = delayRepository;
        this.stationRepository = stationRepository;
        this.weatherService = weatherService;
        this.delayInfoCache = delayInfoCache;
        this.trainStatusCache = trainStatusCache;
        this.mapper = delayMapper;
        this.scheduler = scheduler;
    }

    public void processDelays(Flux<DelayInfo> delayInfos) {
        delayInfos
            .flatMap(delayInfo -> {
                if (StringUtils.isText(delayInfo.getStationCode()) && !stationRepository.existsById(delayInfo.getStationCode()).block()) {
                    stationRepository.save(TrainStationEntity.builder().stationCode(delayInfo.getStationCode()).build()).block();
                }
                return Mono.just(delayInfo);
            })
            .flatMap(delayInfo -> {
                if (!StringUtils.isAnyText(delayInfo.getActualArrival(), delayInfo.getActualDeparture())) {
                    return trainStatusCache
                            .markIncomplete(delayInfo.getTrainNumber(), delayInfo.getDate())
                            .then(Mono.empty());
                } else {
                    Mono<Void> markCompleteMono = StringUtils.isText(delayInfo.getActualArrival())
                            ? trainStatusCache.markComplete(delayInfo.getTrainNumber(), delayInfo.getDate())
                            : Mono.empty();

                    return markCompleteMono.thenReturn(delayInfo);
                }
            })
            .flatMap(delayInfo -> delayInfoCache.isDuplicate(delayInfo)
                .flatMap(duplicate -> {
                    if (duplicate) {
                        return Mono.empty();
                    }
                    return delayInfoCache.cacheDelay(delayInfo).thenReturn(delayInfo);
                })
            )
            .flatMap(delayInfo -> {
                return weatherService.getWeatherInfo(delayInfo.getStationCode(), getTimeForWeatherForecast(delayInfo))
                        .flatMap(weatherInfo -> Mono.fromCallable(() -> {
                            DelayEntity delayEntity = mapper.apiToEntity(delayInfo);
                            delayEntity = mapper.addWeatherData(delayEntity, weatherInfo);
                            return delayEntity;
                        }))
                        .onErrorResume(throwable -> {
                            LOG.warn("Could not get weather info for {}", delayInfo.getStationCode(), throwable);
                            return Mono.just(mapper.apiToEntity(delayInfo));
                        });
            })
            .flatMap(delayRepository::save)
            .subscribeOn(scheduler)
            .subscribe();
    }

    private LocalDateTime getTimeForWeatherForecast(DelayInfo delayInfo) {
        if (delayInfo.getActualArrival() != null && delayInfo.getActualArrival().contains(":")) {
            String[] split = delayInfo.getActualArrival().split(":");
            return delayInfo.getDate().atTime(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
        } else if (delayInfo.getActualDeparture() != null && delayInfo.getActualDeparture().contains(":")) {
            String[] split = delayInfo.getActualDeparture().split(":");
            return delayInfo.getDate().atTime(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
        } else {
            throw new RuntimeException("Could not retrieve time from delay info for weather forecast");
        }
    }
}
