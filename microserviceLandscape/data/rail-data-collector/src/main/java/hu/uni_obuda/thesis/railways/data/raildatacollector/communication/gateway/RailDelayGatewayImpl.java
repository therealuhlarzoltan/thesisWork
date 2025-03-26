package hu.uni_obuda.thesis.railways.data.raildatacollector.communication.gateway;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.client.RailDelayWebClient;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ShortTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ShortTrainDetailsResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
public class RailDelayGatewayImpl implements RailDelayGateway {

    private final RailDelayWebClient webClient;

    @CircuitBreaker(name = "getTimetableApi", fallbackMethod = "handleTimetableFallback")
    @Retry(name = "getTimetableApi")
    @TimeLimiter(name = "getTimetableApi", fallbackMethod = "handleTimetableTimeoutFallback")
    public Mono<ShortTimetableResponse> getShortTimetable(String from, String to) {
        return webClient.getShortTimetable(from, to);
    }

    @CircuitBreaker(name = "getTrainDetailsApi", fallbackMethod = "handleDetailsFallback")
    @Retry(name = "getTrainDetailsApi")
    @TimeLimiter(name = "getTrainDetailsApi", fallbackMethod = "handleDetailsTimeoutFallback")
    public Mono<ShortTrainDetailsResponse> getShortTrainDetails(String trainUri) {
        return webClient.getShortTrainDetails(trainUri);
    }


    public Mono<ShortTimetableResponse> handleTimetableFallback(String from, String to, Exception ex) {
        return Mono.empty();
    }

    public Mono<ShortTimetableResponse> handleTimetableTimeoutFallback(String from, String to, TimeoutException timeoutException) {
        return Mono.empty();
    }

    public Mono<ShortTrainDetailsResponse> handleDetailsFallback(String trainUri, Exception ex) {
        return Mono.empty();
    }

    public Mono<ShortTrainDetailsResponse> handleDetailsTimeoutFallback(String trainUri, TimeoutException timeoutException) {
        return Mono.empty();
    }

}
