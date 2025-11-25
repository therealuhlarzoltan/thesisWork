package hu.uni_obuda.thesis.railways.data.geocodingservice.worker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.event.CrudEvent;
import hu.uni_obuda.thesis.railways.data.event.Event;
import hu.uni_obuda.thesis.railways.data.event.HttpResponseEvent;
import hu.uni_obuda.thesis.railways.data.geocodingservice.controller.GeocodingController;
import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingRequest;
import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageProcessorTest {

    private static final String BINDING_NAME = "geocodingDataResponses-out-0";

    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private GeocodingController geocodingController;
    @Mock
    private ResponseMessageSender responseMessageSender;

    private Scheduler messageProcessingScheduler;

    private MessageProcessorImpl messageProcessor;

    @BeforeEach
    void setUp() throws JsonProcessingException {

        messageProcessingScheduler = Schedulers.immediate();

        messageProcessor = new MessageProcessorImpl(
                objectMapper,
                geocodingController,
                responseMessageSender,
                messageProcessingScheduler
        );

        doReturn("json").when(objectMapper).writeValueAsString(any());
    }

    @Test
    void givenGetEventWithoutCorrelationId_whenGeocodingSucceeds_thenSuccessResponseIsSent() {
        String address = "Budapest-Nyugati";
        GeocodingRequest request = new GeocodingRequest(address);

        CrudEvent<String, Object> incomingEvent =
                new CrudEvent<>(CrudEvent.Type.GET, address, new Object());

        when(objectMapper.convertValue(any(), eq(GeocodingRequest.class)))
                .thenReturn(request);

        GeocodingResponse geocodingResponse = new GeocodingResponse();
        when(geocodingController.getCoordinates(anyString()))
                .thenReturn(Mono.just(geocodingResponse));

        Message<Event<?, ?>> message = MessageBuilder
                .<Event<?, ?>>withPayload(incomingEvent)
                .build();


        messageProcessor.accept(message);


        verify(geocodingController).getCoordinates(address);
        verify(responseMessageSender)
                .sendResponseMessage(eq(BINDING_NAME), any(HttpResponseEvent.class));
        verifyNoMoreInteractions(responseMessageSender);
    }

    @Test
    void givenGetEventWithCorrelationId_whenGeocodingSucceeds_thenSuccessResponseIsSentWithCorrelationId() {
        String address = "Budapest-Nyugati";
        String correlationId = "corr-123";

        GeocodingRequest request = new GeocodingRequest(address);
        CrudEvent<String, Object> incomingEvent =
                new CrudEvent<>(CrudEvent.Type.GET, address, new Object());

        when(objectMapper.convertValue(any(), eq(GeocodingRequest.class)))
                .thenReturn(request);

        GeocodingResponse geocodingResponse = new GeocodingResponse();
        when(geocodingController.getCoordinates(anyString()))
                .thenReturn(Mono.just(geocodingResponse));

        Message<Event<?, ?>> message = MessageBuilder
                .<Event<?, ?>>withPayload(incomingEvent)
                .setHeader("correlationId", correlationId)
                .build();

        messageProcessor.accept(message);

        verify(geocodingController).getCoordinates(address);
        verify(responseMessageSender)
                .sendResponseMessage(eq(BINDING_NAME), eq(correlationId), any(HttpResponseEvent.class));
        verifyNoMoreInteractions(responseMessageSender);
    }

    @Test
    void givenGetEventWithoutCorrelationId_whenGeocodingFails_thenErrorResponseIsSent() {
        String address = "Budapest-Nyugati";
        GeocodingRequest request = new GeocodingRequest(address);

        CrudEvent<String, Object> incomingEvent =
                new CrudEvent<>(CrudEvent.Type.GET, address, new Object());

        when(objectMapper.convertValue(any(), eq(GeocodingRequest.class)))
                .thenReturn(request);

        when(geocodingController.getCoordinates(anyString()))
                .thenReturn(Mono.error(new RuntimeException("boom")));

        Message<Event<?, ?>> message = MessageBuilder
                .<Event<?, ?>>withPayload(incomingEvent)
                .build();

        messageProcessor.accept(message);

        verify(geocodingController).getCoordinates(address);
        verify(responseMessageSender)
                .sendResponseMessage(eq(BINDING_NAME), any(HttpResponseEvent.class));
        verifyNoMoreInteractions(responseMessageSender);
    }

    @Test
    void givenGetEventWithCorrelationId_whenGeocodingFails_thenErrorResponseIsSentWithCorrelationId() {
        String address = "Budapest-Nyugati";
        String correlationId = "corr-456";
        GeocodingRequest request = new GeocodingRequest(address);

        CrudEvent<String, Object> incomingEvent =
                new CrudEvent<>(CrudEvent.Type.GET, address, new Object());

        when(objectMapper.convertValue(any(), eq(GeocodingRequest.class)))
                .thenReturn(request);

        when(geocodingController.getCoordinates(anyString()))
                .thenReturn(Mono.error(new RuntimeException("boom")));

        Message<Event<?, ?>> message = MessageBuilder
                .<Event<?, ?>>withPayload(incomingEvent)
                .setHeader("correlationId", correlationId)
                .build();

        messageProcessor.accept(message);

        verify(geocodingController).getCoordinates(address);
        verify(responseMessageSender)
                .sendResponseMessage(eq(BINDING_NAME), eq(correlationId), any(HttpResponseEvent.class));
        verifyNoMoreInteractions(responseMessageSender);
    }

    @Test
    void givenEventWithNonStringKey_whenProcessing_thenBadRequestErrorResponseIsSent() {
        CrudEvent<Integer, Object> invalidEvent =
                new CrudEvent<>(CrudEvent.Type.GET, 42, new Object());

        Message<Event<?, ?>> message = MessageBuilder
                .<Event<?, ?>>withPayload(invalidEvent)
                .build();


        messageProcessor.accept(message);

        verify(responseMessageSender)
                .sendResponseMessage(eq(BINDING_NAME), any(HttpResponseEvent.class));
        verifyNoMoreInteractions(responseMessageSender);
        verifyNoInteractions(geocodingController);
    }
}
