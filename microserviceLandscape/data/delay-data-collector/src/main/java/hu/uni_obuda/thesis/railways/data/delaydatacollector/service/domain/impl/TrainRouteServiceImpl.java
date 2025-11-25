package hu.uni_obuda.thesis.railways.data.delaydatacollector.service.domain.impl;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainRouteRequest;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.domain.TrainRouteEntity;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.mapper.TrainRouteMapper;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.repository.domain.TrainRouteRepository;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.service.domain.TrainRouteService;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TrainRouteServiceImpl implements TrainRouteService {

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
        return repository.findById(trainRouteRequest.getTrainNumber())
                .flatMap(existing -> {
                    updateEntity(existing, mapper.apiToEntity(trainRouteRequest));
                    return repository.save(existing);
                })
                .map(mapper::entityToApi)
                .switchIfEmpty(Mono.error(new EntityNotFoundException(trainRouteRequest.getTrainNumber(), TrainRouteEntity.class)));
    }

    @Override
    public Mono<Void> deleteTrainRoute(String trainNumber) {
        return repository.existsById(trainNumber).flatMap(exists -> {
            if (!exists) {
                return Mono.error(new EntityNotFoundException(trainNumber, TrainRouteEntity.class));
            }
            return repository.deleteById(trainNumber).then();
        });
    }

    private void updateEntity(TrainRouteEntity oldEntity, TrainRouteEntity newEntity) {
        oldEntity.setFrom(newEntity.getFrom());
        oldEntity.setTo(newEntity.getTo());
    }
}
