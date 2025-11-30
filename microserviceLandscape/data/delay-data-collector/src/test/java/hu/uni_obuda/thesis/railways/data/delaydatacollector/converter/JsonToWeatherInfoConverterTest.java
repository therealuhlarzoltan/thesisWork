package hu.uni_obuda.thesis.railways.data.delaydatacollector.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import io.r2dbc.spi.Row;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JsonToWeatherInfoConverterTest {

    @Test
    void convert_jsonPresent_returnsWeatherInfo() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        JsonToWeatherInfoConverter converter = new JsonToWeatherInfoConverter(objectMapper);
        Row row = mock(Row.class);
        String json = "{\"temperature\":25}";
        WeatherInfo weatherInfo = mock(WeatherInfo.class);

        when(row.get("weather", String.class)).thenReturn(json);
        when(objectMapper.readValue(json, WeatherInfo.class)).thenReturn(weatherInfo);

        WeatherInfo result = converter.convert(row);

        assertNotNull(result);
        assertSame(weatherInfo, result);
        verify(row).get("weather", String.class);
        verify(objectMapper).readValue(json, WeatherInfo.class);
    }

    @Test
    void convert_jsonIsNull_returnsNull() {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        JsonToWeatherInfoConverter converter = new JsonToWeatherInfoConverter(objectMapper);
        Row row = mock(Row.class);

        when(row.get("weather", String.class)).thenReturn(null);

        WeatherInfo result = converter.convert(row);

        assertNull(result);
        verify(row).get("weather", String.class);
        verifyNoInteractions(objectMapper);
    }

    @Test
    void convert_jsonIsBlank_returnsNull() {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        JsonToWeatherInfoConverter converter = new JsonToWeatherInfoConverter(objectMapper);
        Row row = mock(Row.class);

        when(row.get("weather", String.class)).thenReturn("   ");

        WeatherInfo result = converter.convert(row);

        assertNull(result);
        verify(row).get("weather", String.class);
        verifyNoInteractions(objectMapper);
    }

    @Test
    void convert_conversionFails_throwsRuntimeException() throws JsonProcessingException {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        JsonToWeatherInfoConverter converter = new JsonToWeatherInfoConverter(objectMapper);
        Row row = mock(Row.class);
        String json = "{\"temperature\":\"invalid\"}";
        JsonProcessingException cause = new JsonProcessingException("parse error") {};

        when(row.get("weather", String.class)).thenReturn(json);
        when(objectMapper.readValue(json, WeatherInfo.class)).thenThrow(cause);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> converter.convert(row));

        assertEquals("Failed to convert JSON to WeatherInfo", ex.getMessage());
        assertSame(cause, ex.getCause());
        verify(row).get("weather", String.class);
        verify(objectMapper).readValue(json, WeatherInfo.class);
    }
}
