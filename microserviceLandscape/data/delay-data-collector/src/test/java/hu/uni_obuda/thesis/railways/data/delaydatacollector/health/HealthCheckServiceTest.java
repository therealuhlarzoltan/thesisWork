package hu.uni_obuda.thesis.railways.data.delaydatacollector.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HealthCheckServiceTest {

    @Test
    void getRailDataCollectorHealth_success_returnsStatusUp() throws Exception {
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(uriSpec);
        String baseUrl = "http://rail";
        String healthUri = "/health";
        String fullUrl = baseUrl + healthUri;
        when(uriSpec.uri(fullUrl)).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(Mono.just(ResponseEntity.ok().build()));

        HealthCheckServiceImpl service = new HealthCheckServiceImpl(webClient);
        setField(service, "railDataCollectorUrl", baseUrl);
        setField(service, "healthCheckUri", healthUri);

        Health health = service.getRailDataCollectorHealth().block(Duration.ofSeconds(5));

        assertNotNull(health);
        assertEquals(Health.up().build().getStatus(), health.getStatus());
        verify(webClient).get();
        verify(uriSpec).uri(fullUrl);
        verify(headersSpec).retrieve();
        verify(responseSpec).toBodilessEntity();
    }

    @Test
    void getRailDataCollectorHealth_error_returnsStatusDown() throws Exception {
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(uriSpec);
        String baseUrl = "http://rail";
        String healthUri = "/health";
        String fullUrl = baseUrl + healthUri;
        when(uriSpec.uri(fullUrl)).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(Mono.error(new IllegalStateException("fail")));

        HealthCheckServiceImpl service = new HealthCheckServiceImpl(webClient);
        setField(service, "railDataCollectorUrl", baseUrl);
        setField(service, "healthCheckUri", healthUri);

        Health health = service.getRailDataCollectorHealth().block(Duration.ofSeconds(5));

        assertNotNull(health);
        assertEquals("DOWN", health.getStatus().getCode());
    }

    @Test
    void getWeatherDataCollectorHealth_error_returnsStatusOutOfService() throws Exception {
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(uriSpec);
        String baseUrl = "http://weather";
        String healthUri = "/health";
        String fullUrl = baseUrl + healthUri;
        when(uriSpec.uri(fullUrl)).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(Mono.error(new IllegalStateException("fail")));

        HealthCheckServiceImpl service = new HealthCheckServiceImpl(webClient);
        setField(service, "weatherDataCollectorUrl", baseUrl);
        setField(service, "healthCheckUri", healthUri);

        Health health = service.getWeatherDataCollectorHealth().block(Duration.ofSeconds(5));

        assertNotNull(health);
        assertEquals("OUT_OF_SERVICE", health.getStatus().getCode());
    }

    @Test
    void getGeocodingServiceHealth_success_returnsStatusUp() throws Exception {
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(uriSpec);
        String baseUrl = "http://geo";
        String healthUri = "/health";
        String fullUrl = baseUrl + healthUri;
        when(uriSpec.uri(fullUrl)).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(Mono.just(ResponseEntity.ok().build()));

        HealthCheckServiceImpl service = new HealthCheckServiceImpl(webClient);
        setField(service, "geocodingServiceUrl", baseUrl);
        setField(service, "healthCheckUri", healthUri);

        Health health = service.getGeocodingServiceHealth().block(Duration.ofSeconds(5));

        assertNotNull(health);
        assertEquals(Health.up().build().getStatus(), health.getStatus());
    }

    @Test
    void shouldRetry_differentExceptionTypes_correctBehavior() throws Exception {
        WebClient webClient = mock(WebClient.class);
        HealthCheckServiceImpl service = new HealthCheckServiceImpl(webClient);
        Method method = HealthCheckServiceImpl.class.getDeclaredMethod("shouldRetry", Throwable.class);
        method.setAccessible(true);

        boolean timeoutResult = (boolean) method.invoke(service, new TimeoutException());
        assertTrue(timeoutResult);

        WebClientRequestException requestException = mock(WebClientRequestException.class);
        boolean requestResult = (boolean) method.invoke(service, requestException);
        assertTrue(requestResult);

        WebClientResponseException response5xx = mock(WebClientResponseException.class);
        when(response5xx.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        boolean response5xxResult = (boolean) method.invoke(service, response5xx);
        assertTrue(response5xxResult);

        WebClientResponseException response4xx = mock(WebClientResponseException.class);
        when(response4xx.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        boolean response4xxResult = (boolean) method.invoke(service, response4xx);
        assertFalse(response4xxResult);

        boolean otherResult = (boolean) method.invoke(service, new IllegalStateException());
        assertFalse(otherResult);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
