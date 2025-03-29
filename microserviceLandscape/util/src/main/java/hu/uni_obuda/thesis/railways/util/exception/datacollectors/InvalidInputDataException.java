package hu.uni_obuda.thesis.railways.util.exception.datacollectors;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@RequiredArgsConstructor
@Data
public class InvalidInputDataException extends RuntimeException {

    private final HttpStatusCode statusCode = HttpStatus.UNPROCESSABLE_ENTITY;

    public InvalidInputDataException(String message) {
        super(message);
    }
}
