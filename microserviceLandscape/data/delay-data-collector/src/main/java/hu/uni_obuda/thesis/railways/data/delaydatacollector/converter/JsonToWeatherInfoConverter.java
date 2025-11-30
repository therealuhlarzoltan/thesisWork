package hu.uni_obuda.thesis.railways.data.delaydatacollector.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import io.r2dbc.spi.Row;
import org.springframework.lang.NonNull;

@RequiredArgsConstructor
@ReadingConverter
public class JsonToWeatherInfoConverter implements Converter<Row, WeatherInfo> {

    private final ObjectMapper objectMapper;

    @Override
    public WeatherInfo convert(@NonNull Row row) {
        try {
            String json = row.get("weather", String.class);
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readValue(json, WeatherInfo.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JSON to WeatherInfo", e);
        }
    }
}
