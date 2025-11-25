package hu.uni_obuda.thesis.railways.data.geocodingservice.service;

import hu.uni_obuda.thesis.railways.data.geocodingservice.communication.gateway.MapsGateway;
import hu.uni_obuda.thesis.railways.data.geocodingservice.communication.response.CoordinatesResponse;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.InternalApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.MalformedURLException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class GeocodingServiceTest {

    @Mock
    private MapsGateway mapsGateway;

    @InjectMocks
    private GeocodingServiceImpl geocodingService;

    @Test
    void getCoordinates_success() {
        String address = "Budapest-Nyugati";
        double latitude = 47.4979;
        double longitude = 19.0402;

        var coordinatesResponse = mock(CoordinatesResponse.class);
        when(coordinatesResponse.isPresent()).thenReturn(true);
        when(coordinatesResponse.getLatitude()).thenReturn(latitude);
        when(coordinatesResponse.getLongitude()).thenReturn(longitude);
        when(mapsGateway.getCoordinates(address)).thenReturn(Mono.just(coordinatesResponse));

        StepVerifier.create(geocodingService.getCoordinates(address))
                .assertNext(response -> {
                    assertThat(response.getLatitude()).isEqualTo(latitude);
                    assertThat(response.getLongitude()).isEqualTo(longitude);
                    assertThat(response.getAddress()).isEqualTo(address);
                })
                .verifyComplete();

        verify(mapsGateway, times(1)).getCoordinates(address);
        verifyNoMoreInteractions(mapsGateway);
    }

    @Test
    void getCoordinates_fallbackOnError_returnsNullCoordinates() throws MalformedURLException {
        String address = "Some Address 123";

        when(mapsGateway.getCoordinates(address))
                .thenReturn(Mono.error(new InternalApiException(URI.create("http://internal/error").toURL())));

        StepVerifier.create(geocodingService.getCoordinates(address))
                .assertNext(response -> {
                    assertThat(response.getLatitude()).isNull();
                    assertThat(response.getLongitude()).isNull();
                    assertThat(response.getAddress()).isEqualTo(address);
                })
                .verifyComplete();

        verify(mapsGateway, times(1)).getCoordinates(address);
        verifyNoMoreInteractions(mapsGateway);
    }
}