package hu.uni_obuda.thesis.railways.data.delaydatacollector.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import io.r2dbc.spi.Parameter;
import org.springframework.lang.NonNull;

@WritingConverter
public class WeatherInfoToJsonConverter implements Converter<WeatherInfo, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convert(@NonNull WeatherInfo source) {
        try {
            return objectMapper.writeValueAsString(source);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert WeatherInfo to JSON", e);
        }
    }
}
