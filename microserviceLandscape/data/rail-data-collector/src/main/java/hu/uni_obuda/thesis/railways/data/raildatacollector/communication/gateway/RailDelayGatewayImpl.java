package hu.uni_obuda.thesis.railways.data.raildatacollector.communication.gateway;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.client.RailDelayWebClient;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ElviraShortTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ElviraShortTrainDetailsResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ElviraTimetableResponse;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.time.LocalDate;

@Profile("data-source-elvira")
@Component
@RequiredArgsConstructor
public class RailDelayGatewayImpl implements RailDelayGateway {

    private static final Logger LOG = LoggerFactory.getLogger(RailDelayGatewayImpl.class);

    private final RailDelayWebClient webClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;

    @Override
    public Mono<ElviraShortTimetableResponse> getShortTimetable(String from, String to, LocalDate date) {
        LOG.debug("Called short timetable gateway with parameters {}, {}, {}", from, to, date);
        return webClient.getShortTimetable(from, to, date)
                .transformDeferred(RateLimiterOperator.of(rateLimiterRegistry.rateLimiter("getTimetableApi")))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreakerRegistry.circuitBreaker("getTimetableApi")))
                .transformDeferred(RetryOperator.of(retryRegistry.retry("getTimetableApi")))
                .onErrorResume(this::handleFallback);
    }

    @Override
    public Mono<ElviraShortTrainDetailsResponse> getShortTrainDetails(String trainUri) {
        LOG.debug("Called train details gateway with uri {}", trainUri);
        return webClient.getShortTrainDetails(trainUri)
                .transformDeferred(RateLimiterOperator.of(rateLimiterRegistry.rateLimiter("getTrainDetailsApi")))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreakerRegistry.circuitBreaker("getTrainDetailsApi")))
                .transformDeferred(RetryOperator.of(retryRegistry.retry("getTrainDetailsApi")))
                .onErrorResume(this::handleFallback);
    }

    @Override
    public Mono<ElviraTimetableResponse> getTimetable(String from, String to, LocalDate date) {
        LOG.debug("Called full timetable gateway with parameters {}, {}, {}", from, to, date);
        return webClient.getTimetable(from, to, date)
                .transformDeferred(RateLimiterOperator.of(rateLimiterRegistry.rateLimiter("getFullTimetableApi")))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreakerRegistry.circuitBreaker("getFullTimetableApi")))
                .transformDeferred(RetryOperator.of(retryRegistry.retry("getFullTimetableApi")))
                .onErrorResume(this::handleFallback);
    }

    private <T> Mono<T> handleFallback(Throwable throwable) {
        if (throwable instanceof CallNotPermittedException callNotPermittedException) {
            LOG.error("Circuit breaker is open", callNotPermittedException);
        } else if (throwable instanceof RequestNotPermitted requestNotPermittedException) {
            LOG.error("Rate limit is exceeded", requestNotPermittedException);
        }
        ApiException apiException;
        try {
            apiException = resolveApiException(throwable);
        } catch (MalformedURLException e) {
            LOG.error("Encountered a Malformed URL while trying to resolve API Exception", e);
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
