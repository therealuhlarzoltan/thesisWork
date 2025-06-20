package hu.uni_obuda.thesis.railways.data.geocodingservice.communication.gateway;

import hu.uni_obuda.thesis.railways.data.geocodingservice.communication.client.MapsWebClient;
import hu.uni_obuda.thesis.railways.data.geocodingservice.communication.response.CoordinatesResponse;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ApiException;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ExternalApiException;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.InternalApiException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;

@Slf4j
@RequiredArgsConstructor
@Component
public class MapsGatewayImpl implements MapsGateway {

    private final MapsWebClient webClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    @Override
    public Mono<CoordinatesResponse> getCoordinates(String address) {
        return webClient.getCoordinates(address)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreakerRegistry.circuitBreaker("geocodingApi")))
                .transformDeferred(RetryOperator.of(retryRegistry.retry("geocodingApi")))
                .onErrorResume(this::handleFallback);
    }

    private <T> Mono<T> handleFallback(Throwable throwable) {
        if (throwable instanceof CallNotPermittedException callNotPermittedException) {
            log.error("Circuit breaker is open", callNotPermittedException);
        }
        ApiException apiException;
        try {
            apiException = resolveApiException(throwable);
        } catch (MalformedURLException e) {
            log.error("Encountered a Malformed URL while trying to resolve API Exception", e);
            apiException = new InternalApiException("Encountered a Malformed URL while trying to resolve API Exception", null);
        }
        return Mono.error(apiException);
    }

    private ApiException resolveApiException(Throwable throwable) throws MalformedURLException {
        if (throwable instanceof WebClientResponseException response) {
            return new ExternalApiException(response.getStatusCode(), response.getRequest().getURI().toURL());
        } else if (throwable instanceof WebClientRequestException request) {
            return new InternalApiException(request.getMessage(), request.getUri().toURL());
        } else if (throwable instanceof ExternalApiException externalApiException) {
            return externalApiException;
        } else if (throwable instanceof InternalApiException internalApiException) {
            return internalApiException;
        }
        else {
            return new InternalApiException("A runtime exception occurred", null);
        }
    }

}
