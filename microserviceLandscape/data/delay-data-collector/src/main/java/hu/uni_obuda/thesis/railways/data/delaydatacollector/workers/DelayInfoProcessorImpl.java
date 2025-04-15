package hu.uni_obuda.thesis.railways.data.delaydatacollector.workers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.event.Event;
import hu.uni_obuda.thesis.railways.data.event.HttpResponseEvent;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;

@RequiredArgsConstructor
public class DelayInfoProcessorImpl implements DelayInfoProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(DelayInfoProcessorImpl.class);

    private final ObjectMapper objectMapper;
    private final WeatherInfoRegistry registry;
    private final IncomingMessageSink messageSink;

    @Override
    public void accept(Message<Event<?, ?>> eventMessage) {

    }

    private HttpResponseEvent retriveHttpResponseEvent(Message<Event<?, ?>> eventMessage) {

    }

    private WeatherInfo retriveWeatherInfo(HttpResponseEvent httpResponseEvent) {
        return deserializeObject(httpResponseEvent.getData().getMessage(), WeatherInfo.class);
    }

    private <T> T deserializeObject(String json, Class<T> clazz) {
        T obj = null;
        try {
            obj = objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException ex) {
            LOG.error("Couldn't deserialize object from json", ex);
            return null;
        }
        return obj;
    }
}
