package hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.messaging.sender;

import hu.uni_obuda.thesis.railways.data.event.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class MessageSenderImpl implements MessageSender {

    private final StreamBridge streamBridge;

    @Override
    public void sendMessage(String bindingName, Event<?, ?> event) {
        log.info("Sending a message to {}", bindingName);
        Message message = MessageBuilder.withPayload(event)
                .setHeader("partitionKey", event.getKey())
                .build();
        if (!streamBridge.send(bindingName, message)) {
            log.error("Failed to send the message to {}", bindingName);
        } else {
            log.info("Successfully sent a message to {}", bindingName);
        }
    }

    @Override
    public void sendMessage(String bindingName, String correlationId, Event<?, ?> event) {
        if (correlationId == null) {
            log.warn("No correlationId found in the message headers, will not send the message");
            return;
        }
        log.info("Sending a  message to {} with correlationId {}", bindingName, correlationId);
        Message message = MessageBuilder.withPayload(event)
                .setHeader("correlationId", correlationId)
                .setHeader("partitionKey", event.getKey())
                .build();
        if (!streamBridge.send(bindingName, message)) {
            log.error("Failed to send the  message to {} with correlationId {}", bindingName, correlationId);
        } else {
            log.info("Successfully sent a message to {} with correlationId {}", bindingName, correlationId);
        }
    }
}
