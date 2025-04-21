package hu.uni_obuda.thesis.railways.util.exception.datacollectors;

import java.net.URL;
import java.util.Objects;

public class InternalApiException extends ApiException {

    public InternalApiException() {
        super();

    }

    public InternalApiException(URL url) {
        super(url);
    }

    public InternalApiException(String message, URL url) {
        super(message, url);
    }

    public InternalApiException(String message, Throwable cause, URL url) {
        super(message, cause, url);
    }

    public InternalApiException(Throwable cause, URL url) {
        super(cause, url);
    }

    public InternalApiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, URL url) {
        super(message, cause, enableSuppression, writableStackTrace, url);
    }

    @Override
    public String getMessage() {
        return "Couldn't send request to " + Objects.toString(url, "Unknown external host") + " error message: " + super.getMessage();
    }
}
