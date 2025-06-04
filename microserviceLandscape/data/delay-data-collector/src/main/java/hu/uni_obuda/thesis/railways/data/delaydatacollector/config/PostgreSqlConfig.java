package hu.uni_obuda.thesis.railways.data.delaydatacollector.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.converters.JsonToWeatherInfoConverter;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.converters.JsonWeatherInfoConverter;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.converters.WeatherInfoToJsonConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;

import java.util.List;

@RequiredArgsConstructor
@Configuration
public class PostgreSqlConfig {

    private final ObjectMapper objectMapper;

    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        return new R2dbcCustomConversions(
                CustomConversions.StoreConversions.NONE,
                List.of(
                        new WeatherInfoToJsonConverter(objectMapper),
                        new JsonToWeatherInfoConverter(objectMapper),
                        new JsonWeatherInfoConverter(objectMapper)
                )
        );
    }
}

