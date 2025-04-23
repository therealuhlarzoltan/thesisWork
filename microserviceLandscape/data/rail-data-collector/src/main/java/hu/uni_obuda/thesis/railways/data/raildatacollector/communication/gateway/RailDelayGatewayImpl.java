package hu.uni_obuda.thesis.railways.data.raildatacollector.communication.gateway;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.client.RailDelayWebClient;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ShortTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ShortTrainDetailsResponse;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ApiException;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ExternalApiException;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.InternalApiException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
public class RailDelayGatewayImpl implements RailDelayGateway {

    private final RailDelayWebClient webClient;

    @CircuitBreaker(name = "getTimetableApi", fallbackMethod = "handleTimetableFallback")
    @Retry(name = "getTimetableApi")
    public Mono<ShortTimetableResponse> getShortTimetable(String from, String to, LocalDate date) {
        return webClient.getShortTimetable(from, to, date);
    }

    @CircuitBreaker(name = "getTrainDetailsApi", fallbackMethod = "handleDetailsFallback")
    @Retry(name = "getTrainDetailsApi")
    public Mono<ShortTrainDetailsResponse> getShortTrainDetails(String trainUri) {
        return webClient.getShortTrainDetails(trainUri);
    }

    public Mono<ShortTimetableResponse> handleTimetableFallback(String from, String to, LocalDate date, Throwable throwable) throws MalformedURLException {
        return Mono.error(resolveApiException(throwable));
    }

    public Mono<ShortTrainDetailsResponse> handleDetailsFallback(String trainUri, Throwable throwable) throws MalformedURLException {
        return Mono.error(resolveApiException(throwable));
    }

    private ApiException resolveApiException(Throwable throwable) throws MalformedURLException {
        if (throwable instanceof WebClientResponseException response) {
            return new ExternalApiException(response.getStatusCode(), response.getRequest().getURI().toURL());
        } else if (throwable instanceof WebClientRequestException request) {
            return new InternalApiException(request.getMessage(), request.getUri().toURL());
        } else {
            return new InternalApiException("A runtime exception occurred", null);
        }
    }
}
