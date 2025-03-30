package hu.uni_obuda.thesis.railways.data.weatherdatacollector.service;

import hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.gateway.MapsGateway;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.gateway.WeatherDataGateway;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.communication.response.WeatherResponse;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static hu.uni_obuda.thesis.railways.data.weatherdatacollector.util.WeatherResponseUtils.*;

@Service
@RequiredArgsConstructor
public class WeatherDataServiceImpl implements WeatherDataService {

    private final MapsGateway mapsGateway;
    private final WeatherDataGateway weatherGateway;

    @Override
    public Mono<WeatherInfo> getWeatherInfoByAddress(String address, LocalDateTime dateTime) {
        return mapsGateway.getCoordinates(address)
                .flatMap(coordinates -> weatherGateway.getWeatherByCoordinates(coordinates.getLatitude(), coordinates.getLongitude(), dateTime.toLocalDate()))
                .map(weatherResponse -> constructWeatherInfoFromResponse(weatherResponse, address, dateTime));

    }

    private WeatherInfo constructWeatherInfoFromResponse(WeatherResponse weatherResponse, String address, LocalDateTime dateTime) {
        if (!weatherResponse.isPresent()) {
            return WeatherInfo.builder()
                    .address(address)
                    .time(dateTime)
                    .build();
        } else {
            return WeatherInfo.builder()
                    .address(address)
                    .latitude(weatherResponse.getLatitude())
                    .longitude(weatherResponse.getLongitude())
                    .time(dateTime)
                    .cloudCoverPercentage(extractCloudCover(weatherResponse, dateTime))
                    .visibilityInMeters(extractVisibility(weatherResponse, dateTime))
                    .windSpeedAt10m(extractWindSpeedAt10m(weatherResponse, dateTime))
                    .windSpeedAt80m(extractWindSpeedAt80m(weatherResponse, dateTime))
                    .temperature(extractTemperature(weatherResponse, dateTime))
                    .relativeHumidity(extractRelativeHumidity(weatherResponse, dateTime))
                    .rain(extractRain(weatherResponse, dateTime))
                    .showers(extractShowers(weatherResponse, dateTime))
                    .precipitation(extreactPrecipitation(weatherResponse, dateTime))
                    .snowDepth(extractSnowDepth(weatherResponse, dateTime))
                    .snowFall(extractSnowFall(weatherResponse, dateTime))
                    .isSnowing(extractIsSnowing(weatherResponse, dateTime))
                    .isRaining(extractIsRaining(weatherResponse, dateTime))
                    .build();
        }

    }
}
