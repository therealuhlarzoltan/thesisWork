package hu.uni_obuda.thesis.railways.data.delaydatacollector.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@EnableAsync
@EnableScheduling
@RequiredArgsConstructor
@Configuration
public class ApplicationConfig {

    @Value("${app.messaging.sending.threadPoolSize:10}")
    Integer messageSenderThreadPoolSize;
    @Value("${app.messaging.sending.taskQueueSize:100}")
    Integer messageSenderTaskQueueSize;

    @Value("${app.data.processing.delay.threadPoolSize:10}")
    Integer delayProcessorThreadPoolSize;
    @Value("${app.data.processing.delay.taskQueueSize:100}")
    Integer delayProcessorTaskQueueSize;

    private final ReactorLoadBalancerExchangeFilterFunction lbFunction;

    @Bean(name = "messageSenderScheduler")
    public Scheduler messageScheduler() {
        return Schedulers.newBoundedElastic(messageSenderThreadPoolSize, messageSenderTaskQueueSize, "message-sender-pool");
    }

    @Bean(name = "trainDelayProcessorScheduler")
    public Scheduler delayProcessorScheduler() {
        return Schedulers.newBoundedElastic(delayProcessorThreadPoolSize, delayProcessorTaskQueueSize, "delay-processor-pool");
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // makes it ISO8601
    }

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.filter(lbFunction).build();
    }
}
