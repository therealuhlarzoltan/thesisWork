package hu.uni_obuda.thesis.railways.util.exception.handler;

import hu.uni_obuda.thesis.railways.util.exception.datacollectors.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.ZonedDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidInputDataException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ResponseEntity<ApiError> handleInvalidInputDataException(InvalidInputDataException e) {
        return new ResponseEntity<>(createApiError(e, HttpStatus.UNPROCESSABLE_ENTITY), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ApiError> handleNotFoundException(EntityNotFoundException e) {
        return new ResponseEntity<>(createApiError(e, HttpStatus.NOT_FOUND), HttpStatus.NOT_FOUND);
    }


    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ResponseEntity<ApiError> handleIllegalArgumentException(IllegalArgumentException e) {
        return new ResponseEntity<>(createApiError(e, HttpStatus.UNPROCESSABLE_ENTITY), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ResponseEntity<ApiError> handleIllegalStateException(IllegalStateException e) {
        return new ResponseEntity<>(createApiError(e, HttpStatus.UNPROCESSABLE_ENTITY), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiError> handleConstraintViolationException(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream().findFirst().map(ConstraintViolation::getMessage).orElse("Constraint violation");
        return new ResponseEntity<>(createApiError(msg, HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String msg = ex.getAllErrors().get(0).getDefaultMessage();
        return createApiError(msg, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<ApiError> handleExternalApiException(ExternalApiException e) {
        var error = createApiError(e.getMessage(), HttpStatus.valueOf(e.getStatusCode().value()));
        return new ResponseEntity<>(error, HttpStatus.valueOf(e.getStatusCode().value()));
    }

    @ExceptionHandler(InternalApiException.class)
    public ResponseEntity<ApiError> handleInternalApiException(InternalApiException e) {
        var error = createApiError(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ExternalApiFormatMismatchException.class)
    public ResponseEntity<ApiError> handleExternalApiFormatMismatchException(ExternalApiFormatMismatchException e) {
        var error = createApiError(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /*@ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleException(Exception e) {
        return createApiError("An unhandled exception occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }*/


    private ApiError createApiError(Exception ex, HttpStatus status) {
        log.info("Returning error response: {}, with status: {}", ex.getMessage(), status);
        return new ApiError(ex.getMessage(), status, ZonedDateTime.now());
    }

    private ApiError createApiError(String message, HttpStatus status) {
        log.info("Returning error response: {}, with status: {}", message, status);
        return new ApiError(message, status, ZonedDateTime.now());
    }
}
