package hu.uni_obuda.thesis.railways.data.weatherdatacollector.workers;

import hu.uni_obuda.thesis.railways.data.event.HttpResponseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
}
