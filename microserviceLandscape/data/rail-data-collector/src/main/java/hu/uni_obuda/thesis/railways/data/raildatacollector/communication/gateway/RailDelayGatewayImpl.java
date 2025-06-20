package hu.uni_obuda.thesis.railways.data.raildatacollector.communication.gateway;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.client.RailDelayWebClient;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ShortTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ShortTrainDetailsResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.TimetableResponse;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ApiException;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ExternalApiException;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.InternalApiException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class RailDelayGatewayImpl implements RailDelayGateway {

    private static final Logger LOG = LoggerFactory.getLogger(RailDelayGatewayImpl.class);

    private final RailDelayWebClient webClient;

    @CircuitBreaker(name = "getTimetableApi", fallbackMethod = "handleTimetableFallback")
    @Retry(name = "getTimetableApi")
    @Override
    public Mono<ShortTimetableResponse> getShortTimetable(String from, String to, LocalDate date) {
        LOG.debug("Called short timetable gateway with parameters {}, {}, {}", from, to, date);
        return webClient.getShortTimetable(from, to, date);
    }

    @CircuitBreaker(name = "getTrainDetailsApi", fallbackMethod = "handleDetailsFallback")
    @Retry(name = "getTrainDetailsApi")
    @Override
    public Mono<ShortTrainDetailsResponse> getShortTrainDetails(String trainUri) {
        LOG.debug("Called train details gateway with uri {}", trainUri);
        return webClient.getShortTrainDetails(trainUri);
    }

    @CircuitBreaker(name = "getFullTimetableApi", fallbackMethod = "handleFullTimetableFallback")
    @Retry(name = "getFullTimetableApi")
    @Override
    public Mono<TimetableResponse> getTimetable(String from, String to, LocalDate date) {
        LOG.debug("Called full timetable gateway with parameters {}, {}, {}", from, to, date);
        return webClient.getTimetable(from, to, date);
    }

    public Mono<ShortTimetableResponse> handleTimetableFallback(String from, String to, LocalDate date, Throwable throwable) throws MalformedURLException {
        return Mono.error(resolveApiException(throwable));
    }

    public Mono<ShortTrainDetailsResponse> handleDetailsFallback(String trainUri, Throwable throwable) throws MalformedURLException {
        return Mono.error(resolveApiException(throwable));
    }

    public Mono<TimetableResponse> handleFullTimetableFallback(String from, String to, LocalDate date, Throwable throwable) throws MalformedURLException {
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
