package hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.messaging.processor;

import hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.DelayRecord;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.service.data.DelayService;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.messaging.sender.MessageSender;
import hu.uni_obuda.thesis.railways.data.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class DataRequestProcessorImpl implements DataRequestProcessor {

    private final DelayService delayService;
    private final MessageSender responseSender;
    private final Scheduler messageProcessingScheduler;

    @Value("${app.messaging.batch.size:200}")
    private Integer batchSize;

    @Override
    public void accept(Message<Event<?, ?>> eventMessage) {
        log.debug("Received message wth id {}", eventMessage.getHeaders().getId());
        processMessage(eventMessage);
    }

    private DataTransferEvent<List<DelayRecord>> retrieveDataTransferEvent(Event<?, ?> genericEvent) {
        if (!(genericEvent instanceof DataTransferEvent<?> dataTransferEvent)) {
            log.error("Unexpected event parameters, expected a DataTransferEvent");
            return null;
        }
        if (dataTransferEvent.getKey() == null) {
            log.error("Unexpected event parameters, expected a String as key");
            return null;
        }
        return new DataTransferEvent<>(dataTransferEvent.getEventType(), dataTransferEvent.getKey(), new ArrayList<>());

    }

    private void processMessage(Message<Event<?, ?>> message) {
        log.info("Processing message created at {}...", message.getPayload().getEventCreatedAt());
        DataTransferEvent<List<DelayRecord>> dataTransferEvent = retrieveDataTransferEvent(message.getPayload());
        if (dataTransferEvent == null) {
            handleIncorrectEventParametersError(message.getPayload());
            return;
        }
        DataTransferEvent.Type eventType = dataTransferEvent.getEventType();
        switch (eventType) {
            case REQUEST -> {
                Flux<DataTransferEvent<List<DelayRecord>>> dataTransferFlux = delayService.getBatches(batchSize, dataTransferEvent.getKey());
                dataTransferFlux
                        .doOnNext(event -> responseSender.sendMessage("dataRequestProcessor-out-0", event))
                        .onErrorContinue((throwable, badValue) ->
                                log.error("Could not map one DelayEntity to DelayRecord, skipping it", throwable)
                        )
                        .subscribeOn(messageProcessingScheduler)
                        .subscribe();
            }
            case null, default -> handleIncorrectEventTypeError(dataTransferEvent);
        }
    }

    private void handleIncorrectEventParametersError(Event<?, ?> event) {
        log.error("Event received with key {} was not of type DataTransferEvent<List<DelayRecord>>", event.getKey());
    }

    private void handleIncorrectEventTypeError(DataTransferEvent<?> dataTransferEvent) {
        log.error("The received DataTransferEvent with key {} did not contain an expected event type", dataTransferEvent.getKey());
    }
}
