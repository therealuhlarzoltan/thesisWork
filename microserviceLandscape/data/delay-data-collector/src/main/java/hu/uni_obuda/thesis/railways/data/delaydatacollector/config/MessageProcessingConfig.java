package hu.uni_obuda.thesis.railways.data.delaydatacollector.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.TrainStatusCache;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.service.DelayService;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.messaging.IncomingMessageSink;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.messaging.processors.CoordinateProcessorImpl;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.messaging.processors.DataRequestProcessorImpl;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.messaging.processors.DelayInfoProcessorImpl;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.messaging.processors.WeatherInfoProcessorImpl;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.messaging.senders.MessageSender;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.registry.CoordinatesRegistry;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.registry.WeatherInfoRegistry;
import hu.uni_obuda.thesis.railways.data.event.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
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
    private final MessageSender messageSender;

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
    public Consumer<Message<Event<?, ?>>> geocodingResponseProcessor(CoordinatesRegistry registry) {
        return new CoordinateProcessorImpl(objectMapper, registry, messageSink);
    }

    @DependsOn("messageProcessingScheduler")
    @Lazy
    @Bean
    public Consumer<Message<Event<?, ?>>> dataRequestProcessor(@Qualifier("messageProcessingScheduler") Scheduler messageProcessingScheduler, DelayService delayService) {
        return new DataRequestProcessorImpl(delayService, messageSender, messageProcessingScheduler);
    }

    @Bean
    public ApplicationRunner runner(IncomingMessageSink sink, DelayService service) {
        return args -> {
            service.processDelays(sink.getDelaySink().asFlux());
        };
    }
}
