package hu.uni_obuda.thesis.railways.data.delaydatacollector.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.*;
import hu.uni_obuda.thesis.railways.data.event.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@RequiredArgsConstructor
@Configuration
public class MessageProcessingConfig {

    private final ObjectMapper objectMapper;
    private final IncomingMessageSink messageSink;

    @Bean
    public Consumer<Message<Event<?, ?>>> delayInfoProcessor() {
        return new DelayInfoProcessorImpl(objectMapper, messageSink);
    }

    @Bean
    public Consumer<Message<Event<?, ?>>> weatherInfoProcessor(WeatherInfoRegistry registry) {
        return new WeatherInfoProcessorImpl(objectMapper, registry, messageSink);
    }
}
