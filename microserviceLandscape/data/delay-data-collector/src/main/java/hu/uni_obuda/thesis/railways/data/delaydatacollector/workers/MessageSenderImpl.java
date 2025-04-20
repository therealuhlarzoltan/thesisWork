package hu.uni_obuda.thesis.railways.data.delaydatacollector.workers;

import hu.uni_obuda.thesis.railways.data.event.Event;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class MessageSenderImpl implements MessageSender {

    private static final Logger LOG = LoggerFactory.getLogger(MessageSenderImpl.class);

    private final StreamBridge streamBridge;

    @Override
    public void sendMessage(String bindingName, Event<?, ?> event) {
        LOG.info("Sending a message to {}", bindingName);
        Message message = MessageBuilder.withPayload(event)
                .setHeader("partitionKey", event.getKey())
                .build();

        if (!streamBridge.send(bindingName, message)) {
            LOG.error("Failed to send the message to {}", bindingName);
        } else {
            LOG.info("Successfully sent a message to {}", bindingName);
        }
    }

    @Override
    public void sendMessage(String bindingName, String correlationId, Event<?, ?> event) {
        if (correlationId == null) {
            LOG.warn("No correlationId found in the message headers, will not send the message");
            return;
        }
        LOG.info("Sending a  message to {} with correlationId {}", bindingName, correlationId);
        Message message = MessageBuilder.withPayload(event)
                .setHeader("correlationId", correlationId)
                .setHeader("partitionKey", event.getKey())
                .build();
        if (!streamBridge.send(bindingName, message)) {
            LOG.error("Failed to send the  message to {} with correlationId {}", bindingName, correlationId);
        } else {
            LOG.info("Successfully sent a message to {} with correlationId {}", bindingName, correlationId);
        }
    }
}
