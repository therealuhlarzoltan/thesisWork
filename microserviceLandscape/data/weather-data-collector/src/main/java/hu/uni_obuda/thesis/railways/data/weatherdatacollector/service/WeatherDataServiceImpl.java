package hu.uni_obuda.thesis.railways.data.weatherdatacollector.service;

import hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.client.WeatherDataWebClient;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.gateway.MapsGateway;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.gateway.WeatherDataGateway;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WeatherDataServiceImpl implements WeatherDataService {

    private final MapsGateway mapsGateway;
    private final WeatherDataGateway weatherGateway;

    @Override
    public Mono<WeatherInfo> getWeatherInfoByAddress(String address, LocalDateTime localDateTime) {
        return null;
    }

}
