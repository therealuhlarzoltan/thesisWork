package hu.uni_obuda.thesis.railways.data.delaydatacollector.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import io.r2dbc.postgresql.codec.Json;
import io.r2dbc.spi.Parameter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.lang.NonNull;

@RequiredArgsConstructor
@WritingConverter
public class WeatherInfoToJsonConverter implements Converter<WeatherInfo, Object> {

    private final ObjectMapper objectMapper;

    @Override
    public Object convert(@NonNull WeatherInfo source) {
        try {
            String json = objectMapper.writeValueAsString(source);
            return Json.of(json); // PostgreSQL driver will handle it as JSONB
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize WeatherInfo", e);
        }
    }
}
