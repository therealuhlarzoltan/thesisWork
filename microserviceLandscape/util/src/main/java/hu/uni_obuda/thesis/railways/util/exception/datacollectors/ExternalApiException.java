package hu.uni_obuda.thesis.railways.util.exception.datacollectors;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;

import java.net.URL;
import java.util.Objects;

public class ExternalApiException extends ApiException {

    private HttpStatusCode statusCode;

    public ExternalApiException() {
        super();
    }

    public ExternalApiException(HttpStatusCode statusCode, URL url) {
        super(url);
        this.statusCode = statusCode;
    }

    public ExternalApiException(HttpStatusCode statusCode, URL url, String message) {
        super(message, url);
        this.statusCode = statusCode;
    }

    public ExternalApiException(HttpStatusCode statusCode, URL url, String message, Throwable cause) {
        super(message, cause, url);
        this.statusCode = statusCode;
    }

    public ExternalApiException(HttpStatusCode statusCode, URL url, Throwable cause) {
        super(cause, url);
        this.statusCode = statusCode;
    }

    public ExternalApiException(HttpStatusCode statusCode, URL url, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace, url);
        this.statusCode = statusCode;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }

    @Override
    public String getMessage() {
        return Objects.toString(url, "Unknown external host") + " responded with status code " + statusCode.toString();
    }
}
