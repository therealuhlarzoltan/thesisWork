package hu.uni_obuda.thesis.railways.data.raildatacollector.communication.gateway;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.client.EmmaRailDataWebClient;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.*;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ApiException;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ExternalApiException;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.InternalApiException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.time.LocalDate;

@Profile("data-source-emma")
@Component
@Slf4j
@RequiredArgsConstructor
public class EmmaRailDataGatewayImpl implements EmmaRailDelayGateway {

    private final EmmaRailDataWebClient webClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;

    @Override
    public Mono<EmmaShortTimetableResponse> getShortTimetable(String from, double fromLatitude, double fromLongitude, String to, double toLatitude, double toLongitude, LocalDate date) {
        log.debug("Called short timetable gateway with parameters {}, {}, {}", from, to, date);
        return webClient.getShortTimetable(from, fromLatitude, fromLongitude, to, toLatitude, toLongitude, date)
                .transformDeferred(RateLimiterOperator.of(rateLimiterRegistry.rateLimiter("getTimetableApi")))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreakerRegistry.circuitBreaker("getTimetableApi")))
                .transformDeferred(RetryOperator.of(retryRegistry.retry("getTimetableApi")))
                .onErrorResume(this::handleFallback);
    }

    @Override
    public Mono<EmmaShortTrainDetailsResponse> getShortTrainDetails(String trainId, LocalDate serviceDate) {
        log.debug("Called train details gateway with id {}", trainId);
        return webClient.getShortTrainDetails(trainId, serviceDate)
                .transformDeferred(RateLimiterOperator.of(rateLimiterRegistry.rateLimiter("getTrainDetailsApi")))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreakerRegistry.circuitBreaker("getTrainDetailsApi")))
                .transformDeferred(RetryOperator.of(retryRegistry.retry("getTrainDetailsApi")))
                .onErrorResume(this::handleFallback);
    }

    @Override
    public Mono<EmmaTimetableResponse> getTimetable(String from, double fromLatitude, double fromLongitude, String to, double toLatitude, double toLongitude, LocalDate date) {
        log.debug("Called full timetable gateway with parameters {}, {}, {}", from, to, date);
        return webClient.getTimetable(from, fromLatitude, fromLongitude, to, toLatitude, toLongitude, date)
                .transformDeferred(RateLimiterOperator.of(rateLimiterRegistry.rateLimiter("getFullTimetableApi")))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreakerRegistry.circuitBreaker("getFullTimetableApi")))
                .transformDeferred(RetryOperator.of(retryRegistry.retry("getFullTimetableApi")))
                .onErrorResume(this::handleFallback);
    }

    private <T> Mono<T> handleFallback(Throwable throwable) {
        if (throwable instanceof CallNotPermittedException callNotPermittedException) {
            log.error("Circuit breaker is open", callNotPermittedException);
        } else if (throwable instanceof RequestNotPermitted requestNotPermittedException) {
            log.error("Rate limit is exceeded", requestNotPermittedException);
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
        } else {
            return new InternalApiException("A runtime exception occurred", null);
        }
    }
}
