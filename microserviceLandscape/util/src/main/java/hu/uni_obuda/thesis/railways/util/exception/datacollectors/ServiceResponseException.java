package hu.uni_obuda.thesis.railways.util.exception.datacollectors;

public class ServiceResponseException extends RuntimeException {

  public ServiceResponseException() {

  }

  public ServiceResponseException(String message) {
    super(message);
  }

  public ServiceResponseException(String message, Throwable cause) {
    super(message, cause);
  }

  public ServiceResponseException(Throwable cause) {
    super(cause);
  }

  public ServiceResponseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
