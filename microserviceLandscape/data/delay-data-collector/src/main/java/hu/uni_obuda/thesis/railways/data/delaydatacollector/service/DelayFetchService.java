package hu.uni_obuda.thesis.railways.data.delaydatacollector.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface DelayFetchService {
    void fetchDelay(String trainNumber, String from, String to, LocalDate date);
    void fetchDelay(String correlationId, String trainNumber, String from, String to, LocalDate date);
}
