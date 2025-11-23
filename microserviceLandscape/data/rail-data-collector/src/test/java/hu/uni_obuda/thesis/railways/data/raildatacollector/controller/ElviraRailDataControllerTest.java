package hu.uni_obuda.thesis.railways.data.raildatacollector.controller;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TrainRouteResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.service.data.ElviraRailDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ElviraRailDataControllerTest {

    @Mock
    private ElviraRailDataService service;

    @InjectMocks
    private ElviraRailDataController testedObject;

    @Test
    void getDelayInfo_delegatesToServiceWithSameArgs() {
        String trainNumber = "12345";
        String from = "BUDAPEST";
        String to = "GYOR";
        LocalDate date = LocalDate.of(2025, 1, 1);

        Flux<DelayInfo> serviceResult = Flux.empty();
        when(service.getDelayInfo(trainNumber, from, to, date)).thenReturn(serviceResult);

        Flux<DelayInfo> result = testedObject.getDelayInfo(trainNumber, from, to, date);

        assertThat(result).isSameAs(serviceResult);
        verify(service, times(1)).getDelayInfo(trainNumber, from, to, date);
        verifyNoMoreInteractions(service);
    }

    @Test
    void planRoute_delegatesToServiceWithSameArgs() {
        String from = "BUDAPEST";
        String to = "DEBRECEN";
        LocalDate date = LocalDate.of(2025, 2, 2);

        Flux<TrainRouteResponse> serviceResult = Flux.empty();
        when(service.planRoute(from, to, date)).thenReturn(serviceResult);

        Flux<TrainRouteResponse> result = testedObject.planRoute(from, to, date);

        assertThat(result).isSameAs(serviceResult);
        verify(service, times(1)).planRoute(from, to, date);
        verifyNoMoreInteractions(service);
    }
}
