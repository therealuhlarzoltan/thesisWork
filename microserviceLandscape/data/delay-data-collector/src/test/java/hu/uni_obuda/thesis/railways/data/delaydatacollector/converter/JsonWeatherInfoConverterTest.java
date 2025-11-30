package hu.uni_obuda.thesis.railways.data.delaydatacollector.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import io.r2dbc.postgresql.codec.Json;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JsonWeatherInfoConverterTest {

    @Test
    void convert_jsonPresent_returnsWeatherInfo() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        JsonWeatherInfoConverter converter = new JsonWeatherInfoConverter(objectMapper);
        Json json = Json.of("{\"temperature\":25}");
        WeatherInfo weatherInfo = mock(WeatherInfo.class);

        when(objectMapper.readValue(json.asString(), WeatherInfo.class)).thenReturn(weatherInfo);

        WeatherInfo result = converter.convert(json);

        assertNotNull(result);
        assertSame(weatherInfo, result);
        verify(objectMapper).readValue(json.asString(), WeatherInfo.class);
    }

    @Test
    void convert_jsonIsBlank_returnsNull() {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        JsonWeatherInfoConverter converter = new JsonWeatherInfoConverter(objectMapper);
        Json json = Json.of("   ");

        WeatherInfo result = converter.convert(json);

        assertNull(result);
        verifyNoInteractions(objectMapper);
    }

    @Test
    void convert_deserializationFails_throwsRuntimeException() throws JsonProcessingException {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        JsonWeatherInfoConverter converter = new JsonWeatherInfoConverter(objectMapper);
        Json json = Json.of("{\"temperature\":\"invalid\"}");
        JsonProcessingException cause = new JsonProcessingException("parse error") {};

        when(objectMapper.readValue(json.asString(), WeatherInfo.class)).thenThrow(cause);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> converter.convert(json));

        assertEquals("Failed to deserialize WeatherInfo from JSONB", ex.getMessage());
        assertSame(cause, ex.getCause());
        verify(objectMapper).readValue(json.asString(), WeatherInfo.class);
    }
}
