package hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.messaging.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.messaging.IncomingMessageSink;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.registry.WeatherInfoRegistry;
import hu.uni_obuda.thesis.railways.data.event.Event;
import hu.uni_obuda.thesis.railways.data.event.HttpResponseEvent;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;

@Slf4j
@RequiredArgsConstructor
public class WeatherInfoProcessorImpl implements WeatherInfoProcessor {

    private final ObjectMapper objectMapper;
    private final WeatherInfoRegistry registry;
    private final IncomingMessageSink messageSink;

    @Override
    public void accept(Message<Event<?, ?>> eventMessage) {
        log.debug("Received message wth id {}", eventMessage.getHeaders().getId());
        processMessage(eventMessage);
    }

    private HttpResponseEvent retrieveHttpResponseEvent(Event<?, ?> genericEvent) {
        if (!(genericEvent instanceof HttpResponseEvent responseEvent)) {
            log.error("Unexpected event parameters, expected a HttpResponseEvent");
            return null;
        }
        return responseEvent;
    }

    private void processMessage(Message<Event<?, ?>> message) {
        log.info("Processing message created at {}...", message.getPayload().getEventCreatedAt());
        HttpResponseEvent responseEvent = retrieveHttpResponseEvent(message.getPayload());
        if (responseEvent == null) {
            log.error("Could not retrieve response event from message: {}", message.getPayload());
            return;
        }
        HttpResponseEvent.Type eventType = responseEvent.getEventType();
        switch (eventType) {
            case SUCCESS -> {
                WeatherInfo response = retrieveWeatherInfo(responseEvent);
                if (response == null) {
                    log.error("Could not retrieve weather info from event: {}", responseEvent);
                    return;
                }
                messageSink.getWeatherSink().tryEmitNext(response);
                registry.onWeatherInfo(response);
            }
            case ERROR -> log.error("Received an error response: {}", retrieveErrorMessage(responseEvent));
            case null, default -> log.error("Received unknown event type: {}", eventType);
        }
    }

    private WeatherInfo retrieveWeatherInfo(HttpResponseEvent httpResponseEvent) {
        return deserializeObject(httpResponseEvent.getData().getMessage(), WeatherInfo.class);
    }

    private String retrieveErrorMessage(HttpResponseEvent httpResponseEvent) {
        if (httpResponseEvent.getEventType() == HttpResponseEvent.Type.ERROR && httpResponseEvent.getData().getMessage() != null) {
            Exception ex = deserializeObject(httpResponseEvent.getData().getMessage(), Exception.class);
            return ex != null ? ex.getMessage() : httpResponseEvent.getData().getMessage();
        } else {
            return "Unexpected error response";
        }
    }

    private <T> T deserializeObject(String json, Class<T> clazz) {
        T obj;
        try {
            obj = objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException ex) {
            log.error("Could not deserialize object from json", ex);
            return null;
        }
        return obj;
    }
}
