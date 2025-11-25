package hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.scheduled;

public interface TrainDelayProcessor {
    void processTrainRoutes();
    void processTrainRoute(String trainNumber);
}
