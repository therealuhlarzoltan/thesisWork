package hu.uni_obuda.thesis.railways.data.delaydatacollector.service;

import java.time.LocalDate;

public interface DelayFetcherService {
    void fetchDelay(String trainNumber, String from, String to, LocalDate date);
    void fetchDelay(String correlationId, String trainNumber, String from, String to, LocalDate date);
}
