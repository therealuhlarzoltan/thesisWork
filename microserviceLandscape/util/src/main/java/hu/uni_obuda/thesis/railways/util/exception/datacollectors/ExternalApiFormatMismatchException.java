package hu.uni_obuda.thesis.railways.util.exception.datacollectors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.net.URL;

@RequiredArgsConstructor
@Getter
public class ExternalApiFormatMismatchException extends RuntimeException {

    private final URL url;

    public ExternalApiFormatMismatchException(String message, Throwable cause, URL url) {
        super(message, cause);
        this.url = url;
    }

    @Override
    public String getMessage() {
        return "Could not parse response from " + url.toString() + " error message: " + super.getMessage();
    }
}
