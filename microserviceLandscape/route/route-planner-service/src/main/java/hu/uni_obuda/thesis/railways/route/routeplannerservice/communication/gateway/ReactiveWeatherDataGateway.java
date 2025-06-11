package hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway;

import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.client.WeatherWebClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Primary
@Component
public class ReactiveWeatherDataGateway implements WeatherDataGateway {

    private final WeatherWebClient webClient;

    public ReactiveWeatherDataGateway(@Qualifier("reactiveWeatherWebClient")WeatherWebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Mono<WeatherInfo> getWeatherInfo(String address, Double latitude, Double longitude, LocalDateTime time) {
        return webClient.makeWeatherRequest(address, latitude, longitude, time);
    }
}
