package hu.uni_obuda.thesis.railways.data.delaydatacollector.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import io.r2dbc.postgresql.codec.Json;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.lang.NonNull;

@ReadingConverter
public class JsonWeatherInfoConverter implements Converter<Json, WeatherInfo> {

    private final ObjectMapper objectMapper;

    public JsonWeatherInfoConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public WeatherInfo convert(@NonNull Json source) {
        try {
            // `source.asString()` returns the JSON text stored in the JSONB column.
            String jsonText = source.asString();
            if (jsonText == null || jsonText.isBlank()) {
                return null;
            }
            return objectMapper.readValue(jsonText, WeatherInfo.class);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to deserialize WeatherInfo from JSONB", ex);
        }
    }
}
