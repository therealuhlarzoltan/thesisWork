package hu.uni_obuda.thesis.railways.data.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
@NoArgsConstructor(force = true)
public class ResponsePayload {
    private final String message;
    private final HttpStatus status;
}