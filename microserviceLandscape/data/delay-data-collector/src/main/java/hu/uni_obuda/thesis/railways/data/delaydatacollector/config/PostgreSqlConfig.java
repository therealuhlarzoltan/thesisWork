package hu.uni_obuda.thesis.railways.data.delaydatacollector.config;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.converters.JsonToWeatherInfoConverter;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.converters.WeatherInfoToJsonConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;

import java.util.List;

@Configuration
public class PostgreSqlConfig {

    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        return new R2dbcCustomConversions(
                CustomConversions.StoreConversions.NONE,
                List.of(
                        new WeatherInfoToJsonConverter(),
                        new JsonToWeatherInfoConverter()
                )
        );
    }
}

