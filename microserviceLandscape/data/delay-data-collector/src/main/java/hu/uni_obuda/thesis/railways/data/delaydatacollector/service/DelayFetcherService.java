package hu.uni_obuda.thesis.railways.data.delaydatacollector.service;

import java.time.LocalDate;

public interface DelayFetcherService {
    void fetchDelay(String trainNumber, String from, double fromLatitude, double fromLongitude, String to, double toLatitude, double toLongitude, LocalDate date);
    void fetchDelay(String correlationId, String trainNumber, String from, double fromLatitude, double fromLongitude, String to, double toLatitude, double toLongitude, LocalDate date);
}
