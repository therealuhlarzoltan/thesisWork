package hu.uni_obuda.thesis.railways.data.raildatacollector.config;

import hu.uni_obuda.thesis.railways.data.event.Event;
import hu.uni_obuda.thesis.railways.data.raildatacollector.workers.MessageProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.function.Consumer;

@RequiredArgsConstructor
@Configuration
public class MessageProcessorConfig {

    private final MessageProcessor messageProcessor;

    @Value("${app.threadPoolSize:10}")
    Integer threadPoolSize;
    @Value("${app.taskQueueSize:100}")
    Integer taskQueueSize;

    @Bean(name = "messageProcessingScheduler")
    public Scheduler messageProcessingScheduler() {
        return Schedulers.newBoundedElastic(threadPoolSize, taskQueueSize, "message-processing-pool");
    }

    @Bean
    public Consumer<Message<Event<?, ?>>> messageProcessor() {
        return messageProcessor;
    }
}
