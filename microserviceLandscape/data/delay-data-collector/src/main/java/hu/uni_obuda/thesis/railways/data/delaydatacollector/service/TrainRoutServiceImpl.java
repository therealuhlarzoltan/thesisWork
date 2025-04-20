package hu.uni_obuda.thesis.railways.data.delaydatacollector.service;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainRouteRequest;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.TrainRouteEntity;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.mapper.TrainRouteMapper;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.repository.TrainRouteRepository;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Service
public class TrainRoutServiceImpl implements TrainRouteService {

    private static final Logger LOG = LoggerFactory.getLogger(TrainRoutServiceImpl.class);

    private final TrainRouteMapper mapper;
    private final TrainRouteRepository repository;

    @Override
    public Mono<TrainRouteResponse> getTrainRoute(String trainNumber) {
        return repository.findById(trainNumber)
                .switchIfEmpty(Mono.error(new EntityNotFoundException(trainNumber, TrainRouteEntity.class))).map(mapper::entityToApi);
    }

    @Override
    public Flux<TrainRouteResponse> getAllTrainRoutes() {
        return repository.findAll().map(mapper::entityToApi);
    }

    @Override
    public Mono<TrainRouteResponse> createTrainRoute(TrainRouteRequest trainRouteRequest) {
        var entity = mapper.apiToEntity(trainRouteRequest);
        return repository.insertTrain(entity.getTrainNumber(), entity.getLineNumber(), entity.getFrom(), entity.getTo()).map(mapper::entityToApi);
    }

    @Override
    public Mono<TrainRouteResponse> updateTrainRoute(TrainRouteRequest trainRouteRequest) {
        TrainRouteEntity existing = repository.findById(trainRouteRequest.getTrainNumber()).block();
        if (existing == null) {
            return Mono.error(new EntityNotFoundException(trainRouteRequest.getTrainNumber(), TrainRouteEntity.class));
        }
        updateEntity(existing, mapper.apiToEntity(trainRouteRequest));
        return repository.save(existing).map(mapper::entityToApi);
    }

    @Override
    public Mono<Void> deleteTrainRoute(String trainNumber) {
        if (!repository.existsById(trainNumber).block().booleanValue()) {
            return Mono.error(new EntityNotFoundException(trainNumber, TrainRouteEntity.class));
        }
        return repository.deleteById(trainNumber);
    }

    private void updateEntity(TrainRouteEntity oldEntity, TrainRouteEntity newEntity) {
        oldEntity.setFrom(newEntity.getFrom());
        oldEntity.setTo(newEntity.getTo());
    }
}
