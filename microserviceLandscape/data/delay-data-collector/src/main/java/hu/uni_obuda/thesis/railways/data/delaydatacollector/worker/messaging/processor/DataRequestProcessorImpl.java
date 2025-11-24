package hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.messaging.processor;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.DelayRecord;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.service.data.DelayService;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.messaging.sender.MessageSender;
import hu.uni_obuda.thesis.railways.data.event.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class DataRequestProcessorImpl implements DataRequestProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(DataRequestProcessorImpl.class);

    private final DelayService delayService;
    private final MessageSender responseSender;
    private final Scheduler messageProcessingScheduler;

    @Value("${app.messaging.batch.size:200}")
    private Integer batchSize;

    @Override
    public void accept(Message<Event<?, ?>> eventMessage) {
        LOG.debug("Received message wth id {}", eventMessage.getHeaders().getId());
        if (eventMessage.getHeaders().containsKey("correlationId")) {
            processMessageWithCorrelationId(eventMessage);
        } else {
            processMessageWithoutCorrelationId(eventMessage);
        }
    }

    @SuppressWarnings("unchecked")
    private DataTransferEvent<List<DelayRecord>> retrieveDataTransferEvent(Event<?, ?> genericEvent) {
        if (!(genericEvent instanceof DataTransferEvent<?> dataTransferEvent)) {
            LOG.error("Unexpected event parameters, expected a DataTransferEvent");
            return null;
        }
        if (!(dataTransferEvent.getKey() instanceof String)) {
            LOG.error("Unexpected event parameters, expected a String as key");
            return null;
        }
        return new DataTransferEvent<>(dataTransferEvent.getEventType(), dataTransferEvent.getKey(), new ArrayList<>());

    }

    private void processMessageWithoutCorrelationId(Message<Event<?, ?>> message) {
        LOG.info("Processing message created at {} with no correlationId...", message.getPayload().getEventCreatedAt());
        DataTransferEvent<List<DelayRecord>> dataTransferEvent = retrieveDataTransferEvent(message.getPayload());
        if (dataTransferEvent == null) {
            handleIncorrectEventParametersError(message.getPayload(), null);
            return;
        }
        DataTransferEvent.Type eventType = dataTransferEvent.getEventType();
        switch (eventType) {
            case REQUEST -> {
                Flux<DataTransferEvent<List<DelayRecord>>> dataTransferFlux = delayService.getBatches(batchSize, dataTransferEvent.getKey());
                dataTransferFlux
                        .doOnNext(event -> responseSender.sendMessage("dataRequestProcessor-out-0", event))
                        .onErrorContinue((throwable, badValue) -> {
                            LOG.error("Could not map one DelayEntity to DelayRecord, skipping it", throwable);
                        })
                        .subscribeOn(messageProcessingScheduler)
                        .subscribe();
            }
            case null, default -> handleIncorrectEventTypeError(dataTransferEvent, null);
        }

    }

    private void processMessageWithCorrelationId(Message<Event<?, ?>> message) {
        String correlationId = message.getHeaders().get("correlationId").toString();
        LOG.info("Processing message created at {} with correlationId {}...", message.getPayload().getEventCreatedAt(), correlationId);
        DataTransferEvent<List<DelayRecord>> dataTransferEvent = retrieveDataTransferEvent(message.getPayload());
        if (dataTransferEvent == null) {
            handleIncorrectEventParametersError(message.getPayload(), correlationId);
            return;
        }
        DataTransferEvent.Type eventType = dataTransferEvent.getEventType();
        switch (eventType) {
            case REQUEST -> {
                Flux<DataTransferEvent<List<DelayRecord>>> dataTransferFlux = delayService.getBatches(batchSize, dataTransferEvent.getKey());
                dataTransferFlux
                        .doOnNext(event -> responseSender.sendMessage("dataRequestProcessor-out-0", correlationId, event))
                        .doOnError(throwable -> {
                            LOG.error("Could not map one DelayEntity to DelayRecord, skipping it", throwable);
                            LOG.error("Was unable to send message to dataResponses-out-0 with correlationId {}", correlationId);
                        })
                        .onErrorResume(throwable -> Mono.empty())
                        .subscribeOn(messageProcessingScheduler)
                        .subscribe();
            }
            case null, default -> handleIncorrectEventTypeError(dataTransferEvent, correlationId);
        }
    }

    private void handleIncorrectEventParametersError(Event<?, ?> event, String correlationId) {
        if (correlationId != null) {
            LOG.error("Event received with key {} and correlationId {} was not of type DataTransferEvent<List<DelayRecord>>", event.getKey(), correlationId);
        } else {
            LOG.error("Event received with key {} was not of type DataTransferEvent<List<DelayRecord>>", event.getKey());
        }
    }

    private void handleIncorrectEventTypeError(DataTransferEvent<?> dataTransferEvent, String correlationId) {
        if (correlationId != null) {
            LOG.error("The received DataTransferEvent with key {} and correlationId {} did not contain an expected event type", dataTransferEvent.getKey(), correlationId);
        } else {
            LOG.error("The received DataTransferEvent with key {} did not contain an expected event type", dataTransferEvent.getKey());
        }
    }
}
