package hu.uni_obuda.thesis.railways.data.delaydatacollector.workers;


import hu.uni_obuda.thesis.railways.data.delaydatacollector.repository.TrainRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class TrainDelayProcessorImpl implements TrainDelayProcessor {

    private final TrainRouteRepository trainRouteRepository;
    private final DelayFetcherService delayFetcherService;
    private final DelayRepository delayRepository; // stores delay records

    private static final String REDIS_PREFIX = "train:complete:";

    @Scheduled(fixedRate = 60_000) // every minute (requires @EnableScheduling)
    public void processTrainRoutes() {
        trainRouteRepository.findAll()
                .flatMap(this::processTrainIfIncomplete)
                .subscribe(); // trigger the chain
    }

    private Mono<Void> processTrainIfIncomplete(TrainRouteEntity trainRoute) {
        String trainId = trainRoute.getTrainNumber();
        String redisKey = REDIS_PREFIX + trainId;

        return redisTemplate.opsForValue().get(redisKey)
                .flatMap(value -> {
                    // Train is already marked as complete, skip processing
                    return Mono.empty();
                })
                .switchIfEmpty(
                        delayFetcherService.getDelaysForRoute(trainRoute)
                                .flatMapMany(Flux::fromIterable) // assuming List<DelayEntity>
                                .flatMap(delayRepository::save)
                                .then(
                                        redisTemplate.opsForValue().set(redisKey, "complete")
                                )
                );
    }
}
