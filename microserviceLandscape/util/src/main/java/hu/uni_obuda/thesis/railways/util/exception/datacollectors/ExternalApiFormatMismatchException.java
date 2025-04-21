package hu.uni_obuda.thesis.railways.util.exception.datacollectors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.net.URL;
import java.util.Objects;

public class ExternalApiFormatMismatchException extends ApiException {

    public ExternalApiFormatMismatchException() {

    }

    public ExternalApiFormatMismatchException(URL url) {
        super(url);
    }

    public ExternalApiFormatMismatchException(String message, URL url) {
        super(message, url);
    }

    public ExternalApiFormatMismatchException(String message, Throwable cause, URL url) {
        super(message, cause, url);
    }

    public ExternalApiFormatMismatchException(Throwable cause, URL url) {
        super(cause, url);
    }

    public ExternalApiFormatMismatchException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, URL url) {
        super(message, cause, enableSuppression, writableStackTrace, url);
    }

    @Override
    public String getMessage() {
        return "Could not parse response from " + Objects.toString(url, "unknown external host") + " error message: " + super.getMessage();
    }
}
