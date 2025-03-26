package hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class WeatherDataWebClientImpl implements WeatherDataWebClient {

    private final WebClient webClient;
}
