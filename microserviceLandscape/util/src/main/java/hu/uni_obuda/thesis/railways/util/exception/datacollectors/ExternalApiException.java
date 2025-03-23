package hu.uni_obuda.thesis.railways.util.exception.datacollectors;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;

import java.net.URL;
import java.util.Objects;

@RequiredArgsConstructor
@Data
public class ExternalApiException extends RuntimeException {

    private final HttpStatusCode statusCode;
    private final URL url;

    @Override
    public String getMessage() {
        return Objects.toString(url, "Unknown external host") + " responded with status code " + statusCode.toString();
    }
}
