package hu.uni_obuda.thesis.railways.route.routeplannerservice.service.impl.common;

import com.github.benmanes.caffeine.cache.Cache;
import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import hu.uni_obuda.thesis.railways.route.routeplannerservice.communication.gateway.GeocodingGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactiveHttpGeocodingServiceTest {

    @Mock
    private GeocodingGateway geocodingGateway;

    @Mock
    private Cache<String, GeocodingResponse> geocodingCache;

    @InjectMocks
    private ReactiveHttpGeocodingService testedObject;

    @Test
    void getCoordinates_returnsCachedValue_whenPresentAndDoesNotCallGatewayOrPutAgain() {
        String station = "Budapest";
        GeocodingResponse cachedResponse = mock(GeocodingResponse.class);

        when(geocodingCache.getIfPresent(station)).thenReturn(cachedResponse);

        StepVerifier.create(testedObject.getCoordinates(station))
                .expectNext(cachedResponse)
                .verifyComplete();

        verify(geocodingCache).getIfPresent(station);
        verifyNoInteractions(geocodingGateway);
        verify(geocodingCache, never()).put(any(), any());
    }

    @Test
    void getCoordinates_callsGatewayOnCacheMiss_andCachesOnSuccess() {
        String station = "Gyor";
        GeocodingResponse gatewayResponse = mock(GeocodingResponse.class);

        when(geocodingCache.getIfPresent(station)).thenReturn(null);
        when(geocodingGateway.getCoordinates(station)).thenReturn(Mono.just(gatewayResponse));

        StepVerifier.create(testedObject.getCoordinates(station))
                .expectNext(gatewayResponse)
                .verifyComplete();

        verify(geocodingCache).getIfPresent(station);
        verify(geocodingGateway).getCoordinates(station);
        verify(geocodingCache).put(eq(station), eq(gatewayResponse));
    }

    @Test
    void getCoordinates_doesNotCache_whenGatewayReturnsEmpty() {
        String station = "Szeged";

        when(geocodingCache.getIfPresent(station)).thenReturn(null);
        when(geocodingGateway.getCoordinates(station)).thenReturn(Mono.empty());

        StepVerifier.create(testedObject.getCoordinates(station))
                .verifyComplete();

        verify(geocodingCache).getIfPresent(station);
        verify(geocodingGateway).getCoordinates(station);
        verify(geocodingCache, never()).put(any(), any());
    }

    @Test
    void getCoordinates_doesNotCache_whenGatewayErrors() {
        String station = "Debrecen";
        RuntimeException error = new RuntimeException("Gateway error");

        when(geocodingCache.getIfPresent(station)).thenReturn(null);
        when(geocodingGateway.getCoordinates(station)).thenReturn(Mono.error(error));

        StepVerifier.create(testedObject.getCoordinates(station))
                .expectErrorMatches(throwable -> throwable == error)
                .verify();

        verify(geocodingCache).getIfPresent(station);
        verify(geocodingGateway).getCoordinates(station);
        verify(geocodingCache, never()).put(any(), any());
    }
}
