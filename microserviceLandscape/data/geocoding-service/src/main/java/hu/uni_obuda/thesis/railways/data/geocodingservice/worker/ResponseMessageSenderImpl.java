package hu.uni_obuda.thesis.railways.data.geocodingservice.worker;

import hu.uni_obuda.thesis.railways.data.event.HttpResponseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ResponseMessageSenderImpl implements ResponseMessageSender {

    private final StreamBridge streamBridge;

    @Override
    public void sendResponseMessage(String bindingName, HttpResponseEvent event) {
        log.info("Sending a response message to {}", bindingName);
        Message<HttpResponseEvent> responseMessage = MessageBuilder.withPayload(event)
                .build();

        if (!streamBridge.send(bindingName, responseMessage)) {
            log.error("Failed to send the response message to {}", bindingName);
        }
    }

    @Override
    public void sendResponseMessage(String bindingName, String correlationId, HttpResponseEvent event) {
        if (correlationId == null) {
            log.warn("No correlationId found in the message headers, will not send a response message");
            return;
        }
        log.info("Sending a response message to {} with correlationId {}", bindingName, correlationId);
        Message<HttpResponseEvent> responseMessage = MessageBuilder.withPayload(event)
                .setHeader("correlationId", correlationId)
                .build();
        if (!streamBridge.send(bindingName, responseMessage)) {
            log.error("Failed to send the response message to {} with correlationId {}", bindingName, correlationId);
        }
    }
}
