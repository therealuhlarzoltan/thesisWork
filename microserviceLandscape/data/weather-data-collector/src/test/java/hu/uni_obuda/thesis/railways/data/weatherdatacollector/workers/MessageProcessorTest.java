package hu.uni_obuda.thesis.railways.data.weatherdatacollector.workers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.event.CrudEvent;
import hu.uni_obuda.thesis.railways.data.event.Event;
import hu.uni_obuda.thesis.railways.data.event.HttpResponseEvent;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.controller.WeatherDataCollector;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfoRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageProcessorTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private WeatherDataCollector weatherDataCollector;

    @Mock
    private ResponseMessageSender responseSender;

    private MessageProcessorImpl createTested() {
        return new MessageProcessorImpl(objectMapper, weatherDataCollector, responseSender, Schedulers.immediate());
    }

    private WeatherInfoRequest buildRequest(LocalDateTime time) {
        return new WeatherInfoRequest("Budapest-Nyugati", 47.5, 19.0, time);
    }

    @Test
    void accept_validGet_sendsSuccessResponse() throws Exception {
        LocalDateTime time = LocalDateTime.of(2024, 10, 10, 10, 0);
        WeatherInfoRequest request = buildRequest(time);

        CrudEvent<String, Object> crudEvent =
                new CrudEvent<>(CrudEvent.Type.GET, request.getStationName(), request);

        when(objectMapper.convertValue(any(), eq(WeatherInfoRequest.class)))
                .thenReturn(request);

        WeatherInfo weatherInfo = WeatherInfo.builder()
                .address(request.getStationName())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .time(time)
                .build();

        when(weatherDataCollector.getWeatherInfo(
                request.getStationName(),
                request.getLatitude(),
                request.getLongitude(),
                request.getTime()
        )).thenReturn(Mono.just(weatherInfo));

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"dummy\":\"json\"}");

        Message<Event<?, ?>> message = (Message) MessageBuilder.withPayload(crudEvent).build();

        MessageProcessorImpl tested = createTested();

        tested.accept(message);

        verify(responseSender, times(1))
                .sendResponseMessage(eq("weatherDataResponses-out-0"), any(HttpResponseEvent.class));
        verifyNoMoreInteractions(responseSender);
    }

    @Test
    void accept_getEmptyFromService_usesFallbackWeatherInfo() throws Exception {
        LocalDateTime time = LocalDateTime.of(2024, 10, 10, 12, 0);
        WeatherInfoRequest request = buildRequest(time);

        CrudEvent<String, Object> crudEvent =
                new CrudEvent<>(CrudEvent.Type.GET, request.getStationName(), request);

        when(objectMapper.convertValue(any(), eq(WeatherInfoRequest.class)))
                .thenReturn(request);

        when(weatherDataCollector.getWeatherInfo(
                request.getStationName(),
                request.getLatitude(),
                request.getLongitude(),
                request.getTime()
        )).thenReturn(Mono.empty());

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"dummy\":\"json\"}");

        Message<Event<?, ?>> message = (Message) MessageBuilder.withPayload(crudEvent).build();

        MessageProcessorImpl tested = createTested();

        tested.accept(message);

        verify(responseSender, times(1))
                .sendResponseMessage(eq("weatherDataResponses-out-0"), any(HttpResponseEvent.class));
        verifyNoMoreInteractions(responseSender);
    }

    @Test
    void accept_nonCrudEvent_triggersIncorrectEventParametersError() throws Exception {
        @SuppressWarnings("unchecked")
        Event<Object, Object> genericEvent = mock(Event.class);
        when(genericEvent.getKey()).thenReturn("key-1");

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"error\":\"msg\"}");

        Message<Event<?, ?>> message = (Message) MessageBuilder.withPayload(genericEvent).build();

        MessageProcessorImpl tested = createTested();

        tested.accept(message);

        verify(responseSender, times(1))
                .sendResponseMessage(eq("railDataResponses-out-0"), any(HttpResponseEvent.class));
        verifyNoMoreInteractions(responseSender);

        verify(objectMapper, never())
                .convertValue(any(), eq(WeatherInfoRequest.class));
    }

    @Test
    void accept_unsupportedEventType_triggersIncorrectEventTypeError() throws Exception {
        LocalDateTime time = LocalDateTime.of(2024, 10, 10, 15, 0);
        WeatherInfoRequest request = buildRequest(time);

        CrudEvent<String, Object> crudEvent =
                new CrudEvent<>(CrudEvent.Type.CREATE, request.getStationName(), request);

        when(objectMapper.convertValue(any(), eq(WeatherInfoRequest.class)))
                .thenReturn(request);

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"error\":\"unsupported\"}");

        Message<Event<?, ?>> message = (Message) MessageBuilder.withPayload(crudEvent).build();

        MessageProcessorImpl tested = createTested();

        tested.accept(message);

        verify(responseSender, times(1))
                .sendResponseMessage(eq("railDataResponses-out-0"), any(HttpResponseEvent.class));
        verifyNoMoreInteractions(responseSender);

        verifyNoInteractions(weatherDataCollector);
    }

    @Test
    void accept_serializeObjectToJsonFails_stillSendsErrorResponse() throws Exception {
        @SuppressWarnings("unchecked")
        Event<Object, Object> genericEvent = mock(Event.class);
        when(genericEvent.getKey()).thenReturn("key-serialize");

        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("fail serialize") {});

        Message<Event<?, ?>> message = (Message) MessageBuilder.withPayload(genericEvent).build();

        MessageProcessorImpl tested = createTested();

        tested.accept(message);

        ArgumentCaptor<HttpResponseEvent> eventCaptor = ArgumentCaptor.forClass(HttpResponseEvent.class);

        verify(responseSender, times(1))
                .sendResponseMessage(eq("railDataResponses-out-0"), eventCaptor.capture());

        HttpResponseEvent sentEvent = eventCaptor.getValue();
        assertThat(sentEvent).isNotNull();
        verifyNoMoreInteractions(responseSender);
    }
}
