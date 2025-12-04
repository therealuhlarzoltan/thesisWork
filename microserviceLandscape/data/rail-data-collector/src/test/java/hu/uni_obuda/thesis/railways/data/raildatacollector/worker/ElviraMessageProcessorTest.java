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
class ElviraMessageProcessorTest {

    @Mock
    private ElviraRailDataCollector elviraRailDataCollector;
    @Mock
    private ResponseMessageSender responseSender;

    private ElviraMessageProcessorImpl testedObject;

    private  URL exceptionSourceURL;

    @BeforeEach
    void setUp() throws MalformedURLException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        exceptionSourceURL = URI.create("http://localhost:8080/Exception").toURL();
        testedObject = new ElviraMessageProcessorImpl(objectMapper, elviraRailDataCollector, responseSender, Schedulers.immediate());
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
    void accept_validCrudEventAndNonEmptyFlux_successResponseSent() {
        DelayInfoRequest request = createRequest("TRAIN1");
        CrudEvent<String, DelayInfoRequest> crudEvent =
                new CrudEvent<>(CrudEvent.Type.GET, "TRAIN1", request);

        DelayInfo delayInfo = DelayInfo.builder().trainNumber("TRAIN1").build();

        when(elviraRailDataCollector.getDelayInfo(anyString(), anyString(), anyString(), any(LocalDate.class)))
                .thenReturn(Flux.just(delayInfo));

        Message<Event<?, ?>> message = buildMessage(crudEvent, Map.of());

        testedObject.accept(message);

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
    void accept_validCrudEventAndEmptyFlux_noResponseSent() {
        DelayInfoRequest request = createRequest("TRAIN2");
        CrudEvent<String, DelayInfoRequest> crudEvent =
                new CrudEvent<>(CrudEvent.Type.GET, "TRAIN2", request);

        when(elviraRailDataCollector.getDelayInfo(anyString(), anyString(), anyString(), any(LocalDate.class)))
                .thenReturn(Flux.empty());

        Message<Event<?, ?>> message = buildMessage(crudEvent, Map.of());

        testedObject.accept(message);

        verify(elviraRailDataCollector, times(1))
                .getDelayInfo(eq(request.getTrainNumber()), eq(request.getFrom()), eq(request.getTo()), eq(request.getDate()));

        verifyNoInteractions(responseSender);
    }

    @Test
    void accept_validCrudEventAndInvalidInputError_errorResponseSentWithBadRequest() {
        DelayInfoRequest request = createRequest("TRAIN3");
        CrudEvent<String, DelayInfoRequest> crudEvent =
                new CrudEvent<>(CrudEvent.Type.GET, "TRAIN3", request);

        InvalidInputDataException exception =
                new InvalidInputDataException("invalid input");

        when(elviraRailDataCollector.getDelayInfo(anyString(), anyString(), anyString(), any(LocalDate.class)))
                .thenReturn(Flux.error(exception));

        Message<Event<?, ?>> message = buildMessage(crudEvent, Map.of());

        testedObject.accept(message);

        ArgumentCaptor<HttpResponseEvent> captor = ArgumentCaptor.forClass(HttpResponseEvent.class);
        verify(responseSender, times(1))
                .sendResponseMessage(eq("railDataResponses-out-0"), captor.capture());

        HttpResponseEvent sentEvent = captor.getValue();
        assertEquals(HttpResponseEvent.Type.ERROR, sentEvent.getEventType());
        assertEquals("TRAIN3", sentEvent.getKey());
        ResponsePayload payload = sentEvent.getData();
        assertNotNull(payload);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, payload.getStatus());
        assertNotNull(payload.getMessage());
    }

    @Test
    void accept_validCrudEventWithMixedSuccessAndExternalError_successAndErrorResponsesSent() {
        DelayInfoRequest request = createRequest("TRAIN4");
        CrudEvent<String, DelayInfoRequest> crudEvent =
                new CrudEvent<>(CrudEvent.Type.GET, "TRAIN4", request);

        DelayInfo delayInfo = DelayInfo.builder().trainNumber("TRAIN4").build();
        ExternalApiException exception =
                new ExternalApiException(HttpStatus.INTERNAL_SERVER_ERROR, exceptionSourceURL);

        when(elviraRailDataCollector.getDelayInfo(anyString(), anyString(), anyString(), any(LocalDate.class)))
                .thenReturn(Flux.concat(Flux.just(delayInfo), Flux.error(exception)));

        Message<Event<?, ?>> message = buildMessage(crudEvent, Map.of());

        testedObject.accept(message);

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
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, second.getData().getStatus());
    }

    @Test
    void accept_nonCrudEvent_incorrectParametersErrorResponseSent() {
        DelayInfoRequest request = createRequest("TRAIN9");
        Event<String, DelayInfoRequest> event = new Event<>(request.getTrainNumber(), request) {
            @Override
             public Enum<?> getEventType() {
                return null;
            }
        };

        Message<Event<?, ?>> message = buildMessage(event, Map.of());

        testedObject.accept(message);

        ArgumentCaptor<HttpResponseEvent> captor = ArgumentCaptor.forClass(HttpResponseEvent.class);
        verify(responseSender, times(1))
                .sendResponseMessage(eq("railDataResponses-out-0"), captor.capture());

        HttpResponseEvent sentEvent = captor.getValue();
        assertEquals(HttpResponseEvent.Type.ERROR, sentEvent.getEventType());
        assertEquals("TRAIN9", sentEvent.getKey());
        assertEquals(HttpStatus.BAD_REQUEST, sentEvent.getData().getStatus());
    }

    @Test
    void accept_crudEventWithNonStringKey_incorrectParametersErrorResponseSent() {
        DelayInfoRequest request = createRequest("TRAIN10");
        CrudEvent<Integer, DelayInfoRequest> crudEvent =
                new CrudEvent<>(CrudEvent.Type.GET, 123, request);

        Message<Event<?, ?>> message = buildMessage(crudEvent, Map.of());

        testedObject.accept(message);

        ArgumentCaptor<HttpResponseEvent> captor = ArgumentCaptor.forClass(HttpResponseEvent.class);
        verify(responseSender, times(1))
                .sendResponseMessage(eq("railDataResponses-out-0"), captor.capture());

        HttpResponseEvent sentEvent = captor.getValue();
        assertEquals(HttpResponseEvent.Type.ERROR, sentEvent.getEventType());
        assertEquals("123", sentEvent.getKey());
        assertEquals(HttpStatus.BAD_REQUEST, sentEvent.getData().getStatus());
    }

    @Test
    void accept_crudEventWithConversionError_incorrectParametersErrorResponseSent() {
        ObjectMapper spyMapper = spy(new ObjectMapper());
        spyMapper.registerModule(new JavaTimeModule());
        ElviraMessageProcessorImpl localProcessor =
                new ElviraMessageProcessorImpl(spyMapper, elviraRailDataCollector, responseSender, Schedulers.immediate());

        DelayInfoRequest request = createRequest("TRAIN11");
        CrudEvent<String, Object> crudEvent =
                new CrudEvent<>(CrudEvent.Type.GET, "TRAIN11", new Object());

        doThrow(new IllegalArgumentException("conversion failed"))
                .when(spyMapper)
                .convertValue(any(), eq(DelayInfoRequest.class));

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
    void accept_validCrudEventAndTrainNotInServiceError_errorResponseSentWithTooEarly() {
        DelayInfoRequest request = createRequest("TRAIN16");
        CrudEvent<String, DelayInfoRequest> crudEvent =
                new CrudEvent<>(CrudEvent.Type.GET, "TRAIN16", request);

        TrainNotInServiceException exception = new TrainNotInServiceException("not in service", LocalDate.now());

        when(elviraRailDataCollector.getDelayInfo(anyString(), anyString(), anyString(), any(LocalDate.class)))
                .thenReturn(Flux.error(exception));

        Message<Event<?, ?>> message = buildMessage(crudEvent, Map.of());

        testedObject.accept(message);

        ArgumentCaptor<HttpResponseEvent> captor = ArgumentCaptor.forClass(HttpResponseEvent.class);
        verify(responseSender, times(1))
                .sendResponseMessage(eq("railDataResponses-out-0"), captor.capture());

        HttpResponseEvent sentEvent = captor.getValue();
        assertEquals(HttpResponseEvent.Type.ERROR, sentEvent.getEventType());
        assertEquals("TRAIN16", sentEvent.getKey());
        assertEquals(HttpStatus.TOO_EARLY, sentEvent.getData().getStatus());
    }

    @Test
    void accept_validCrudEventAndRuntimeError_errorResponseSentWithInternalServerError() {
        DelayInfoRequest request = createRequest("TRAIN17");
        CrudEvent<String, DelayInfoRequest> crudEvent =
                new CrudEvent<>(CrudEvent.Type.GET, "TRAIN17", request);

        RuntimeException exception = new RuntimeException("unexpected");

        when(elviraRailDataCollector.getDelayInfo(anyString(), anyString(), anyString(), any(LocalDate.class)))
                .thenReturn(Flux.error(exception));

        Message<Event<?, ?>> message = buildMessage(crudEvent, Map.of());

        testedObject.accept(message);

        ArgumentCaptor<HttpResponseEvent> captor = ArgumentCaptor.forClass(HttpResponseEvent.class);
        verify(responseSender, times(1))
                .sendResponseMessage(eq("railDataResponses-out-0"), captor.capture());

        HttpResponseEvent sentEvent = captor.getValue();
        assertEquals(HttpResponseEvent.Type.ERROR, sentEvent.getEventType());
        assertEquals("TRAIN17", sentEvent.getKey());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, sentEvent.getData().getStatus());
    }
}
