package hu.uni_obuda.thesis.railways.data.geocodingservice.controller;

import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import hu.uni_obuda.thesis.railways.data.geocodingservice.service.GeocodingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeocodingControllerTest {

    @Mock
    private GeocodingService geocodingService;

    @InjectMocks
    private GeocodingControllerImpl geocodingController;

    @Test
    void getCoordinates_successfulServiceCall_returnsCoordinates() {
        String address = "Budapest-Nyugati";
        GeocodingResponse expected =
                new GeocodingResponse(47.507, 19.045, address);

        when(geocodingService.getCoordinates(address))
                .thenReturn(Mono.just(expected));


        Mono<GeocodingResponse> result = geocodingController.getCoordinates(address);


        StepVerifier.create(result)
                .expectNext(expected)
                .verifyComplete();

        verify(geocodingService, times(1)).getCoordinates(address);
        verifyNoMoreInteractions(geocodingService);
    }

    @Test
    void getCoordinates_failedServiceCall_returnsFallback() {
        String address = "Some invalid address";
        GeocodingResponse expected =
                new GeocodingResponse(null, null, address);

        when(geocodingService.getCoordinates(address))
                .thenReturn(Mono.just(expected));

        Mono<GeocodingResponse> result = geocodingController.getCoordinates(address);

        StepVerifier.create(result)
                .expectNext(expected)
                .verifyComplete();

        verify(geocodingService, times(1)).getCoordinates(address);
        verifyNoMoreInteractions(geocodingService);
    }
}
