package hu.uni_obuda.thesis.railways.data.delaydatacollector.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import io.r2dbc.postgresql.codec.Json;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WeatherInfoToJsonConverterTest {

    @Test
    void convert_serializationSucceeds_returnsJson() throws JsonProcessingException {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        WeatherInfoToJsonConverter converter = new WeatherInfoToJsonConverter(objectMapper);
        WeatherInfo source = mock(WeatherInfo.class);
        when(objectMapper.writeValueAsString(source)).thenReturn("{\"temperature\":25}");

        Object result = converter.convert(source);

        assertNotNull(result);
        assertInstanceOf(Json.class, result);
        assertEquals("{\"temperature\":25}", ((Json) result).asString());
        verify(objectMapper).writeValueAsString(source);
    }

    @Test
    void convert_serializationFails_throwsRuntimeException() throws JsonProcessingException {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        WeatherInfoToJsonConverter converter = new WeatherInfoToJsonConverter(objectMapper);
        WeatherInfo source = mock(WeatherInfo.class);
        JsonProcessingException cause = new JsonProcessingException("fail") {};
        when(objectMapper.writeValueAsString(source)).thenThrow(cause);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> converter.convert(source));

        assertEquals("Failed to serialize WeatherInfo", ex.getMessage());
        assertSame(cause, ex.getCause());
        verify(objectMapper).writeValueAsString(source);
    }
}
