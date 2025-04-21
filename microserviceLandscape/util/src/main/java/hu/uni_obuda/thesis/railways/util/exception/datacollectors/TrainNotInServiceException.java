package hu.uni_obuda.thesis.railways.util.exception.datacollectors;

import java.time.LocalDate;

public class TrainNotInServiceException extends RuntimeException {

    private String trainNumber;
    private LocalDate date;

    public TrainNotInServiceException() {

    }

    public TrainNotInServiceException(String trainNumber, LocalDate date) {
        super("Train with train number " + trainNumber + " is not in service on " + date.toString());
        this.trainNumber = trainNumber;
        this.date = date;
    }

    public TrainNotInServiceException(Throwable cause, String trainNumber, LocalDate date) {
        super("Train with train number " + trainNumber + " is not in service on " + date.toString(), cause);
        this.trainNumber = trainNumber;
        this.date = date;
    }


    public TrainNotInServiceException(Throwable cause, boolean enableSuppression, boolean writableStackTrace, String trainNumber, LocalDate date) {
        super("Train with train number " + trainNumber + " is not in service on " + date.toString(), cause, enableSuppression, writableStackTrace);
        this.trainNumber = trainNumber;
        this.date = date;
    }

    public String getTrainNumber() {
        return trainNumber;
    }

    public LocalDate getDate() {
        return date;
    }
}
