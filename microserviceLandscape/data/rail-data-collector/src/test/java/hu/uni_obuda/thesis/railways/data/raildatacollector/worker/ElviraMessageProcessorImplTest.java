package hu.uni_obuda.thesis.railways.data.raildatacollector.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import hu.uni_obuda.thesis.railways.data.event.CrudEvent;
import hu.uni_obuda.thesis.railways.data.event.Event;
import hu.uni_obuda.thesis.railways.data.event.HttpResponseEvent;
import hu.uni_obuda.thesis.railways.data.event.ResponsePayload;
import hu.uni_obuda.thesis.railways.data.raildatacollector.controller.ElviraRailDataCollector;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfoRequest;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ElviraMessageProcessorImplTest {

    @Mock
    private ElviraRailDataCollector elviraRailDataCollector;

    @Mock
    private ResponseMessageSender responseSender;

    private ObjectMapper objectMapper;

    private ElviraMessageProcessorImpl processor;

    private  URL exceptionSourceURL;


    @BeforeEach
    void setUp() throws MalformedURLException {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        exceptionSourceURL = URI.create("http://localhost:8080/Exception").toURL();
        processor = new ElviraMessageProcessorImpl(objectMapper, elviraRailDataCollector, responseSender, Schedulers.immediate());
    }

    private DelayInfoRequest createRequest(String trainNumber) {
        return new DelayInfoRequest(
                trainNumber,
                "FROM",
                1.0,
                2.0,
                "TO",
                3.0,
                4.0,
                LocalDate.of(2025, 1, 1)
        );
    }

    private Message<Event<?, ?>> buildMessage(Event<?, ?> event, Map<String, Object> headers) {
        MessageBuilder<Event<?, ?>> builder = MessageBuilder.withPayload(event);
        headers.forEach(builder::setHeader);
        return builder.build();
    }

    @Test
    void accept_withoutCorrelationId_validCrudEventAndNonEmptyFlux_successResponseSent() {
        DelayInfoRequest request = createRequest("TRAIN1");
        CrudEvent<String, DelayInfoRequest> crudEvent =
                new CrudEvent<>(CrudEvent.Type.GET, "TRAIN1", request);

        DelayInfo delayInfo = DelayInfo.builder().trainNumber("TRAIN1").build();

        when(elviraRailDataCollector.getDelayInfo(anyString(), anyString(), anyString(), any(LocalDate.class)))
                .thenReturn(Flux.just(delayInfo));

        Message<Event<?, ?>> message = buildMessage(crudEvent, Map.of());

        processor.accept(message);

        verify(elviraRailDataCollector, times(1))
                .getDelayInfo(eq(request.getTrainNumber()), eq(request.getFrom()), eq(request.getTo()), eq(request.getDate()));

        ArgumentCaptor<HttpResponseEvent> captor = ArgumentCaptor.forClass(HttpResponseEvent.class);
        verify(responseSender, times(1))
                .sendResponseMessage(eq("railDataResponses-out-0"), captor.capture());

        HttpResponseEvent sentEvent = captor.getValue();
        assertEquals(HttpResponseEvent.Type.SUCCESS, sentEvent.getEventType());
        assertEquals("TRAIN1", sentEvent.getKey());
        assertNotNull(sentEvent.getData());
        assertEquals(HttpStatus.OK, sentEvent.getData().getStatus());
        assertNotNull(sentEvent.getData());
    }

    @Test
    void accept_withoutCorrelationId_validCrudEventAndEmptyFlux_noResponseSent() {
        DelayInfoRequest request = createRequest("TRAIN2");
        CrudEvent<String, DelayInfoRequest> crudEvent =
                new CrudEvent<>(CrudEvent.Type.GET, "TRAIN2", request);

        when(elviraRailDataCollector.getDelayInfo(anyString(), anyString(), anyString(), any(LocalDate.class)))
                .thenReturn(Flux.empty());

        Message<Event<?, ?>> message = buildMessage(crudEvent, Map.of());

        processor.accept(message);

        verify(elviraRailDataCollector, times(1))
                .getDelayInfo(eq(request.getTrainNumber()), eq(request.getFrom()), eq(request.getTo()), eq(request.getDate()));

        verifyNoInteractions(responseSender);
    }

    @Test
    void accept_withoutCorrelationId_validCrudEventAndInvalidInputError_errorResponseSentWithBadRequest() {
        DelayInfoRequest request = createRequest("TRAIN3");
        CrudEvent<String, DelayInfoRequest> crudEvent =
                new CrudEvent<>(CrudEvent.Type.GET, "TRAIN3", request);

        InvalidInputDataException exception =
                new InvalidInputDataException("invalid input");

        when(elviraRailDataCollector.getDelayInfo(anyString(), anyString(), anyString(), any(LocalDate.class)))
                .thenReturn(Flux.error(exception));

        Message<Event<?, ?>> message = buildMessage(crudEvent, Map.of());

        processor.accept(message);

        ArgumentCaptor<HttpResponseEvent> captor = ArgumentCaptor.forClass(HttpResponseEvent.class);
        verify(responseSender, times(1))
                .sendResponseMessage(eq("railDataResponses-out-0"), captor.capture());

        HttpResponseEvent sentEvent = captor.getValue();
        assertEquals(HttpResponseEvent.Type.ERROR, sentEvent.getEventType());
        assertEquals("TRAIN3", sentEvent.getKey());
        ResponsePayload payload = sentEvent.getData();
        assertNotNull(payload);
        assertEquals(HttpStatus.BAD_REQUEST, payload.getStatus());
        assertNotNull(payload.getMessage());
    }

    @Test
    void accept_withoutCorrelationId_validCrudEventWithMixedSuccessAndExternalError_successAndErrorResponsesSent() {
        DelayInfoRequest request = createRequest("TRAIN4");
        CrudEvent<String, DelayInfoRequest> crudEvent =
                new CrudEvent<>(CrudEvent.Type.GET, "TRAIN4", request);

        DelayInfo delayInfo = DelayInfo.builder().trainNumber("TRAIN4").build();
        ExternalApiException exception =
                new ExternalApiException(HttpStatus.INTERNAL_SERVER_ERROR, exceptionSourceURL);

        when(elviraRailDataCollector.getDelayInfo(anyString(), anyString(), anyString(), any(LocalDate.class)))
                .thenReturn(Flux.concat(Flux.just(delayInfo), Flux.error(exception)));

        Message<Event<?, ?>> message = buildMessage(crudEvent, Map.of());

        processor.accept(message);

        ArgumentCaptor<HttpResponseEvent> captor = ArgumentCaptor.forClass(HttpResponseEvent.class);
        verify(responseSender, times(2))
                .sendResponseMessage(eq("railDataResponses-out-0"), captor.capture());

        assertEquals(2, captor.getAllValues().size());
        HttpResponseEvent first = captor.getAllValues().get(0);
        HttpResponseEvent second = captor.getAllValues().get(1);

        assertEquals(HttpResponseEvent.Type.SUCCESS, first.getEventType());
        assertEquals("TRAIN4", first.getKey());
        assertEquals(HttpStatus.OK, first.getData().getStatus());

        assertEquals(HttpResponseEvent.Type.ERROR, second.getEventType());
        assertEquals("TRAIN4", second.getKey());
        assertEquals(HttpStatus.BAD_GATEWAY, second.getData().getStatus());
    }

    @Test
    void accept_withCorrelationId_validCrudEventAndNonEmptyFlux_successResponseSentWithCorrelationId() {
        DelayInfoRequest request = createRequest("TRAIN5");
        CrudEvent<String, DelayInfoRequest> crudEvent =
                new CrudEvent<>(CrudEvent.Type.GET, "TRAIN5", request);

        DelayInfo delayInfo = DelayInfo.builder().trainNumber("TRAIN5").build();

        when(elviraRailDataCollector.getDelayInfo(anyString(), anyString(), anyString(), any(LocalDate.class)))
                .thenReturn(Flux.just(delayInfo));

        Message<Event<?, ?>> message = buildMessage(crudEvent, Map.of("correlationId", "CID-1"));

        processor.accept(message);

        verify(elviraRailDataCollector, times(1))
                .getDelayInfo(eq(request.getTrainNumber()), eq(request.getFrom()), eq(request.getTo()), eq(request.getDate()));

        ArgumentCaptor<HttpResponseEvent> captor = ArgumentCaptor.forClass(HttpResponseEvent.class);
        verify(responseSender, times(1))
                .sendResponseMessage(eq("railDataResponses-out-0"), eq("CID-1"), captor.capture());

        HttpResponseEvent sentEvent = captor.getValue();
        assertEquals(HttpResponseEvent.Type.SUCCESS, sentEvent.getEventType());
        assertEquals("TRAIN5", sentEvent.getKey());
        assertEquals(HttpStatus.OK, sentEvent.getData().getStatus());
    }

    @Test
    void accept_withCorrelationId_validCrudEventAndEmptyFlux_noResponseSent() {
        DelayInfoRequest request = createRequest("TRAIN6");
        CrudEvent<String, DelayInfoRequest> crudEvent =
                new CrudEvent<>(CrudEvent.Type.GET, "TRAIN6", request);

        when(elviraRailDataCollector.getDelayInfo(anyString(), anyString(), anyString(), any(LocalDate.class)))
                .thenReturn(Flux.empty());

        Message<Event<?, ?>> message = buildMessage(crudEvent, Map.of("correlationId", "CID-2"));

        processor.accept(message);

        verify(elviraRailDataCollector, times(1))
                .getDelayInfo(eq(request.getTrainNumber()), eq(request.getFrom()), eq(request.getTo()), eq(request.getDate()));

        verifyNoInteractions(responseSender);
    }

    @Test
    void accept_withCorrelationId_validCrudEventAndFormatError_errorResponseSentWithBadGateway() {
        DelayInfoRequest request = createRequest("TRAIN7");
        CrudEvent<String, DelayInfoRequest> crudEvent =
                new CrudEvent<>(CrudEvent.Type.GET, "TRAIN7", request);

        ExternalApiFormatMismatchException exception =
                new ExternalApiFormatMismatchException(exceptionSourceURL);

        when(elviraRailDataCollector.getDelayInfo(anyString(), anyString(), anyString(), any(LocalDate.class)))
                .thenReturn(Flux.error(exception));

        Message<Event<?, ?>> message = buildMessage(crudEvent, Map.of("correlationId", "CID-3"));

        processor.accept(message);

        ArgumentCaptor<HttpResponseEvent> captor = ArgumentCaptor.forClass(HttpResponseEvent.class);
        verify(responseSender, times(1))
                .sendResponseMessage(eq("railDataResponses-out-0"), eq("CID-3"), captor.capture());

        HttpResponseEvent sentEvent = captor.getValue();
        assertEquals(HttpResponseEvent.Type.ERROR, sentEvent.getEventType());
        assertEquals("TRAIN7", sentEvent.getKey());
        assertEquals(HttpStatus.BAD_GATEWAY, sentEvent.getData().getStatus());
    }

    @Test
    void accept_withCorrelationId_validCrudEventWithMixedSuccessAndInternalError_successAndErrorResponsesSentWithCorrelationId() {
        DelayInfoRequest request = createRequest("TRAIN8");
        CrudEvent<String, DelayInfoRequest> crudEvent =
                new CrudEvent<>(CrudEvent.Type.GET, "TRAIN8", request);

        DelayInfo delayInfo = DelayInfo.builder().trainNumber("TRAIN8").build();
        InternalApiException exception = new InternalApiException(exceptionSourceURL);

        when(elviraRailDataCollector.getDelayInfo(anyString(), anyString(), anyString(), any(LocalDate.class)))
                .thenReturn(Flux.concat(Flux.just(delayInfo), Flux.error(exception)));

        Message<Event<?, ?>> message = buildMessage(crudEvent, Map.of("correlationId", "CID-4"));

        processor.accept(message);

        ArgumentCaptor<HttpResponseEvent> captor = ArgumentCaptor.forClass(HttpResponseEvent.class);
        verify(responseSender, times(2))
                .sendResponseMessage(eq("railDataResponses-out-0"), eq("CID-4"), captor.capture());

        assertEquals(2, captor.getAllValues().size());
        HttpResponseEvent first = captor.getAllValues().get(0);
        HttpResponseEvent second = captor.getAllValues().get(1);

        assertEquals(HttpResponseEvent.Type.SUCCESS, first.getEventType());
        assertEquals("TRAIN8", first.getKey());
        assertEquals(HttpStatus.OK, first.getData().getStatus());

        assertEquals(HttpResponseEvent.Type.ERROR, second.getEventType());
        assertEquals("TRAIN8", second.getKey());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, second.getData().getStatus());
    }

    @Test
    void accept_withoutCorrelationId_nonCrudEvent_incorrectParametersErrorResponseSent() {
        DelayInfoRequest request = createRequest("TRAIN9");
        Event<String, DelayInfoRequest> event = new Event<>(request.getTrainNumber(), request) {
            @Override
             public Enum<?> getEventType() {
                return null;
            }
        };

        Message<Event<?, ?>> message = buildMessage(event, Map.of());

        processor.accept(message);

        ArgumentCaptor<HttpResponseEvent> captor = ArgumentCaptor.forClass(HttpResponseEvent.class);
        verify(responseSender, times(1))
                .sendResponseMessage(eq("railDataResponses-out-0"), captor.capture());

        HttpResponseEvent sentEvent = captor.getValue();
        assertEquals(HttpResponseEvent.Type.ERROR, sentEvent.getEventType());
        assertEquals("TRAIN9", sentEvent.getKey());
        assertEquals(HttpStatus.BAD_REQUEST, sentEvent.getData().getStatus());
    }

    @Test
    void accept_withoutCorrelationId_crudEventWithNonStringKey_incorrectParametersErrorResponseSent() {
        DelayInfoRequest request = createRequest("TRAIN10");
        CrudEvent<Integer, DelayInfoRequest> crudEvent =
                new CrudEvent<>(CrudEvent.Type.GET, 123, request);

        Message<Event<?, ?>> message = buildMessage(crudEvent, Map.of());

        processor.accept(message);

        ArgumentCaptor<HttpResponseEvent> captor = ArgumentCaptor.forClass(HttpResponseEvent.class);
        verify(responseSender, times(1))
                .sendResponseMessage(eq("railDataResponses-out-0"), captor.capture());

        HttpResponseEvent sentEvent = captor.getValue();
        assertEquals(HttpResponseEvent.Type.ERROR, sentEvent.getEventType());
        assertEquals("123", sentEvent.getKey());
        assertEquals(HttpStatus.BAD_REQUEST, sentEvent.getData().getStatus());
    }

    @Test
     void accept_withoutCorrelationId_crudEventWithConversionError_incorrectParametersErrorResponseSent() {
        ObjectMapper spyMapper = spy(new ObjectMapper());
        spyMapper.registerModule(new JavaTimeModule());
        ElviraMessageProcessorImpl localProcessor =
                new ElviraMessageProcessorImpl(spyMapper, elviraRailDataCollector, responseSender, Schedulers.immediate());

        DelayInfoRequest request = createRequest("TRAIN11");
        CrudEvent<String, Object> crudEvent =
                new CrudEvent<>(CrudEvent.Type.GET, "TRAIN11", new Object());

        when(spyMapper.convertValue(any(), eq(DelayInfoRequest.class)))
                .thenThrow(new IllegalArgumentException("conversion failed"));

        Message<Event<?, ?>> message = buildMessage(crudEvent, Map.of());

        localProcessor.accept(message);

        ArgumentCaptor<HttpResponseEvent> captor = ArgumentCaptor.forClass(HttpResponseEvent.class);
        verify(responseSender, times(1))
                .sendResponseMessage(eq("railDataResponses-out-0"), captor.capture());

        HttpResponseEvent sentEvent = captor.getValue();
        assertEquals(HttpResponseEvent.Type.ERROR, sentEvent.getEventType());
        assertEquals("TRAIN11", sentEvent.getKey());
        assertEquals(HttpStatus.BAD_REQUEST, sentEvent.getData().getStatus());
    }

    @Test
    void accept_withCorrelationId_nonCrudEvent_incorrectParametersErrorResponseSentWithCorrelationId() {
        DelayInfoRequest request = createRequest("TRAIN12");
        Event<String, DelayInfoRequest> event = new Event<>(request.getTrainNumber(), request) {
            @Override
             public Enum<?> getEventType() {
                return null;
            }
        };

        Message<Event<?, ?>> message = buildMessage(event, Map.of("correlationId", "CID-5"));

        processor.accept(message);

        ArgumentCaptor<HttpResponseEvent> captor = ArgumentCaptor.forClass(HttpResponseEvent.class);
        verify(responseSender, times(1))
                .sendResponseMessage(eq("railDataResponses-out-0"), eq("CID-5"), captor.capture());

        HttpResponseEvent sentEvent = captor.getValue();
        assertEquals(HttpResponseEvent.Type.ERROR, sentEvent.getEventType());
        assertEquals("TRAIN12", sentEvent.getKey());
        assertEquals(HttpStatus.BAD_REQUEST, sentEvent.getData().getStatus());
    }

    @Test
    void accept_withCorrelationId_crudEventWithNonStringKey_incorrectParametersErrorResponseSentWithCorrelationId() {
        DelayInfoRequest request = createRequest("TRAIN13");
        CrudEvent<Integer, DelayInfoRequest> crudEvent =
                new CrudEvent<>(CrudEvent.Type.GET, 456, request);

        Message<Event<?, ?>> message = buildMessage(crudEvent, Map.of("correlationId", "CID-6"));

        processor.accept(message);

        ArgumentCaptor<HttpResponseEvent> captor = ArgumentCaptor.forClass(HttpResponseEvent.class);
        verify(responseSender, times(1))
                .sendResponseMessage(eq("railDataResponses-out-0"), eq("CID-6"), captor.capture());

        HttpResponseEvent sentEvent = captor.getValue();
        assertEquals(HttpResponseEvent.Type.ERROR, sentEvent.getEventType());
        assertEquals("456", sentEvent.getKey());
        assertEquals(HttpStatus.BAD_REQUEST, sentEvent.getData().getStatus());
    }

    @Test
     void accept_withCorrelationId_crudEventWithConversionError_incorrectParametersErrorResponseSentWithCorrelationId() {
        ObjectMapper spyMapper = spy(new ObjectMapper());
        spyMapper.registerModule(new JavaTimeModule());
        ElviraMessageProcessorImpl localProcessor =
                new ElviraMessageProcessorImpl(spyMapper, elviraRailDataCollector, responseSender, Schedulers.immediate());

        CrudEvent<String, Object> crudEvent =
                new CrudEvent<>(CrudEvent.Type.GET, "TRAIN14", new Object());

        when(spyMapper.convertValue(any(), eq(DelayInfoRequest.class)))
                .thenThrow(new IllegalArgumentException("conversion failed"));

        Message<Event<?, ?>> message = buildMessage(crudEvent, Map.of("correlationId", "CID-7"));

        localProcessor.accept(message);

        ArgumentCaptor<HttpResponseEvent> captor = ArgumentCaptor.forClass(HttpResponseEvent.class);
        verify(responseSender, times(1))
                .sendResponseMessage(eq("railDataResponses-out-0"), eq("CID-7"), captor.capture());

        HttpResponseEvent sentEvent = captor.getValue();
        assertEquals(HttpResponseEvent.Type.ERROR, sentEvent.getEventType());
        assertEquals("TRAIN14", sentEvent.getKey());
        assertEquals(HttpStatus.BAD_REQUEST, sentEvent.getData().getStatus());
    }

    @Test
    void accept_withCorrelationId_validCrudEventAndApiException_errorResponseSentWithBadGateway() {
        DelayInfoRequest request = createRequest("TRAIN15");
        CrudEvent<String, DelayInfoRequest> crudEvent =
                new CrudEvent<>(CrudEvent.Type.GET, "TRAIN15", request);

        ApiException exception = new ApiException(exceptionSourceURL);

        when(elviraRailDataCollector.getDelayInfo(anyString(), anyString(), anyString(), any(LocalDate.class)))
                .thenReturn(Flux.error(exception));

        Message<Event<?, ?>> message = buildMessage(crudEvent, Map.of("correlationId", "CID-8"));

        processor.accept(message);

        ArgumentCaptor<HttpResponseEvent> captor = ArgumentCaptor.forClass(HttpResponseEvent.class);
        verify(responseSender, times(1))
                .sendResponseMessage(eq("railDataResponses-out-0"), eq("CID-8"), captor.capture());

        HttpResponseEvent sentEvent = captor.getValue();
        assertEquals(HttpResponseEvent.Type.ERROR, sentEvent.getEventType());
        assertEquals("TRAIN15", sentEvent.getKey());
        assertEquals(HttpStatus.BAD_GATEWAY, sentEvent.getData().getStatus());
    }

    @Test
    void accept_withoutCorrelationId_validCrudEventAndTrainNotInServiceError_errorResponseSentWithTooEarly() {
        DelayInfoRequest request = createRequest("TRAIN16");
        CrudEvent<String, DelayInfoRequest> crudEvent =
                new CrudEvent<>(CrudEvent.Type.GET, "TRAIN16", request);

        TrainNotInServiceException exception = new TrainNotInServiceException("not in service", LocalDate.now());

        when(elviraRailDataCollector.getDelayInfo(anyString(), anyString(), anyString(), any(LocalDate.class)))
                .thenReturn(Flux.error(exception));

        Message<Event<?, ?>> message = buildMessage(crudEvent, Map.of());

        processor.accept(message);

        ArgumentCaptor<HttpResponseEvent> captor = ArgumentCaptor.forClass(HttpResponseEvent.class);
        verify(responseSender, times(1))
                .sendResponseMessage(eq("railDataResponses-out-0"), captor.capture());

        HttpResponseEvent sentEvent = captor.getValue();
        assertEquals(HttpResponseEvent.Type.ERROR, sentEvent.getEventType());
        assertEquals("TRAIN16", sentEvent.getKey());
        assertEquals(HttpStatus.TOO_EARLY, sentEvent.getData().getStatus());
    }

    @Test
    void accept_withoutCorrelationId_validCrudEventAndRuntimeError_errorResponseSentWithInternalServerError() {
        DelayInfoRequest request = createRequest("TRAIN17");
        CrudEvent<String, DelayInfoRequest> crudEvent =
                new CrudEvent<>(CrudEvent.Type.GET, "TRAIN17", request);

        RuntimeException exception = new RuntimeException("unexpected");

        when(elviraRailDataCollector.getDelayInfo(anyString(), anyString(), anyString(), any(LocalDate.class)))
                .thenReturn(Flux.error(exception));

        Message<Event<?, ?>> message = buildMessage(crudEvent, Map.of());

        processor.accept(message);

        ArgumentCaptor<HttpResponseEvent> captor = ArgumentCaptor.forClass(HttpResponseEvent.class);
        verify(responseSender, times(1))
                .sendResponseMessage(eq("railDataResponses-out-0"), captor.capture());

        HttpResponseEvent sentEvent = captor.getValue();
        assertEquals(HttpResponseEvent.Type.ERROR, sentEvent.getEventType());
        assertEquals("TRAIN17", sentEvent.getKey());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, sentEvent.getData().getStatus());
    }
}
