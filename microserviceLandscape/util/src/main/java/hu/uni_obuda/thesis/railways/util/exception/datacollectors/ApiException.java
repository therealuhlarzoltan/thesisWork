package hu.uni_obuda.thesis.railways.util.exception.datacollectors;

import java.net.URL;

public class ApiException extends RuntimeException {

    protected final URL url;

    public ApiException(URL url) {
        this.url = url;
    }

    public ApiException(String message, URL url) {
        super(message);
        this.url = url;
    }

    public ApiException(String message, Throwable cause, URL url) {
        super(message, cause);
        this.url = url;
    }

    public ApiException(Throwable cause, URL url) {
        super(cause);
        this.url = url;
    }

    public ApiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, URL url) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.url = url;
    }
}
