package hu.uni_obuda.thesis.railways.util.exception.datacollectors;

public class TrainNotInServiceException extends RuntimeException {

    private final String trainNumber;

    public TrainNotInServiceException(String trainNumber) {
        super("Train with train number " + trainNumber + " is not in service today");
        this.trainNumber = trainNumber;
    }

    public TrainNotInServiceException(Throwable cause, String trainNumber) {
        super("Train with train number " + trainNumber + " is not in service today", cause);
        this.trainNumber = trainNumber;
    }


    public TrainNotInServiceException(Throwable cause, boolean enableSuppression, boolean writableStackTrace, String trainNumber) {
        super("Train with train number " + trainNumber + " is not in service today", cause, enableSuppression, writableStackTrace);
        this.trainNumber = trainNumber;
    }

    public String getTrainNumber() {
        return trainNumber;
    }
}
