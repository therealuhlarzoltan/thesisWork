package hu.uni_obuda.thesis.railways.data.delaydatacollector.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.TrainStatusCache;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.service.DelayService;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.*;
import hu.uni_obuda.thesis.railways.data.event.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.function.Consumer;

@RequiredArgsConstructor
@Configuration
public class MessageProcessingConfig {

    private final ObjectMapper objectMapper;
    private final IncomingMessageSink messageSink;
    private final TrainStatusCache trainStatusCache;

    @Value("${app.messaging.processing.threadPoolSize:10}")
    Integer threadPoolSize;
    @Value("${app.messaging.processing.taskQueueSize:100}")
    Integer taskQueueSize;

    @Bean(name = "messageProcessingScheduler")
    public Scheduler messageScheduler() {
        return Schedulers.newBoundedElastic(threadPoolSize, taskQueueSize, "message-processing-pool");
    }

    @Bean
    public Consumer<Message<Event<?, ?>>> delayInfoProcessor() {
        return new DelayInfoProcessorImpl(objectMapper, messageSink, trainStatusCache);
    }

    @Bean
    public Consumer<Message<Event<?, ?>>> weatherInfoProcessor(WeatherInfoRegistry registry) {
        return new WeatherInfoProcessorImpl(objectMapper, registry, messageSink);
    }

    @Bean
    public ApplicationRunner runner(IncomingMessageSink sink, DelayService service) {
        return args -> {
            service.processDelays(sink.getDelaySink().asFlux());
        };
    }
}
