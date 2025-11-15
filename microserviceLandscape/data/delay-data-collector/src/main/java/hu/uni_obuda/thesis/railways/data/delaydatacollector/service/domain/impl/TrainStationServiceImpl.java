package hu.uni_obuda.thesis.railways.data.delaydatacollector.service.domain.impl;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.TrainStationResponse;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.mapper.TrainStationMapper;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.repository.domain.TrainStationRepository;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.service.domain.TrainStationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Service
public class TrainStationServiceImpl implements TrainStationService {

    private static final Logger LOG = LoggerFactory.getLogger(TrainStationServiceImpl.class);

    private final TrainStationRepository repository;
    private final TrainStationMapper mapper;

    @Override
    public Flux<TrainStationResponse> getTrainStations() {
        LOG.info("Getting all train stations");
        return repository.findAll().map(mapper::entityToApi);
    }

    @Override
    public Mono<TrainStationResponse> getTrainStationById(String id) {
        LOG.info("Getting train station with station code {}", id);
        return repository.findById(id).map(mapper::entityToApi);
    }
}
