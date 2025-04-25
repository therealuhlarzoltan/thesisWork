package hu.uni_obuda.thesis.railways.data.raildatacollector.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.event.Event;
import hu.uni_obuda.thesis.railways.data.raildatacollector.controller.RailDataCollector;
import hu.uni_obuda.thesis.railways.data.raildatacollector.workers.MessageProcessor;
import hu.uni_obuda.thesis.railways.data.raildatacollector.workers.MessageProcessorImpl;
import hu.uni_obuda.thesis.railways.data.raildatacollector.workers.ResponseMessageSender;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.function.Consumer;

@RequiredArgsConstructor
@Configuration
public class MessageProcessorConfig {

    private static final Logger LOG = LoggerFactory.getLogger(MessageProcessorConfig.class);

    private final ObjectMapper objectMapper;
    private final RailDataCollector railDataCollector;
    private final ResponseMessageSender responseSender;
    private MessageProcessor messageProcessor;

    @Value("${app.threadPoolSize:10}")
    Integer threadPoolSize;
    @Value("${app.taskQueueSize:100}")
    Integer taskQueueSize;
    @Value("${railway.api.rate.limit.delay-between-requests:1000}")
    Integer delayBetweenRequests;
    @Value("${railway.api.rate.limit.number-of-concurrent-calls:3}")
    Integer numberOfConcurrentCalls;

    @Bean(name = "messageProcessingScheduler")
    public Scheduler messageProcessingScheduler() {
        return Schedulers.newBoundedElastic(threadPoolSize, taskQueueSize, "message-processing-pool");
    }

    @Bean
    public Consumer<Flux<Message<Event<?, ?>>>> messageProcessor() {
        if (messageProcessor == null) {
            this.messageProcessor = new MessageProcessorImpl(objectMapper, railDataCollector, responseSender, messageProcessingScheduler());
        }
        return flux -> flux
                .publishOn(messageProcessingScheduler())
                .delayElements(Duration.ofMillis(delayBetweenRequests))
                .flatMap(this::processSingleMessage, numberOfConcurrentCalls)
                .onErrorContinue((error, obj) -> {
                    LOG.error("Error during processing: {}", error.getMessage(), error);
                })
                .then()
                .subscribe();
    }

    private Mono<Void> processSingleMessage(Message<Event<?, ?>> eventMessage) {
        return Mono.fromRunnable(() -> messageProcessor.accept(eventMessage))
                .subscribeOn(messageProcessingScheduler())
                .then();
    }
}
