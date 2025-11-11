package hu.uni_obuda.thesis.railways.data.raildatacollector.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.EmmaShortTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ElviraShortTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.entity.ScheduledDateEntity;
import hu.uni_obuda.thesis.railways.data.raildatacollector.entity.ScheduledIntervalEntity;
import hu.uni_obuda.thesis.railways.data.raildatacollector.entity.ScheduledJobEntity;
import hu.uni_obuda.thesis.railways.data.raildatacollector.util.adapter.ReactiveDateRepositoryAdapter;
import hu.uni_obuda.thesis.railways.data.raildatacollector.util.adapter.ReactiveIntervalRepositoryAdapter;
import hu.uni_obuda.thesis.railways.data.raildatacollector.util.adapter.ReactiveJobRepositoryAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.scheduling.annotation.EnableScheduling;

@RequiredArgsConstructor
@EnableScheduling
@Configuration
public class RedisCacheConfig {

    private final ObjectMapper objectMapper;

    @Bean
    public ReactiveRedisTemplate<String, ElviraShortTimetableResponse> timetableResponseRedisTemplate(ReactiveRedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<ElviraShortTimetableResponse> jacksonSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, ElviraShortTimetableResponse.class);

        RedisSerializationContext<String, ElviraShortTimetableResponse> context = RedisSerializationContext
                .<String, ElviraShortTimetableResponse>newSerializationContext(new StringRedisSerializer())
                .value(jacksonSerializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean
    public ReactiveRedisTemplate<String, EmmaShortTimetableResponse> graphQlTimetableResponseRedisTemplate(ReactiveRedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<EmmaShortTimetableResponse> jacksonSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, EmmaShortTimetableResponse.class);

        RedisSerializationContext<String, EmmaShortTimetableResponse> context = RedisSerializationContext
                .<String, EmmaShortTimetableResponse>newSerializationContext(new StringRedisSerializer())
                .value(jacksonSerializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean
    public ReactiveRedisTemplate<String, ScheduledJobEntity> scheduledJobsRedisTemplate(ReactiveRedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<ScheduledJobEntity> jacksonSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, ScheduledJobEntity.class);

        RedisSerializationContext<String, ScheduledJobEntity> context = RedisSerializationContext
                .<String, ScheduledJobEntity>newSerializationContext(new StringRedisSerializer())
                .value(jacksonSerializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean
    public ReactiveRedisTemplate<String, ScheduledDateEntity> scheduledDateRedisTemplate(ReactiveRedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<ScheduledDateEntity> jacksonSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, ScheduledDateEntity.class);

        RedisSerializationContext<String, ScheduledDateEntity> context = RedisSerializationContext
                .<String, ScheduledDateEntity>newSerializationContext(new StringRedisSerializer())
                .value(jacksonSerializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean
    public ReactiveRedisTemplate<String, ScheduledIntervalEntity> scheduledIntervalRedisTemplate(ReactiveRedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<ScheduledIntervalEntity> jacksonSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, ScheduledIntervalEntity.class);

        RedisSerializationContext<String, ScheduledIntervalEntity> context = RedisSerializationContext
                .<String, ScheduledIntervalEntity>newSerializationContext(new StringRedisSerializer())
                .value(jacksonSerializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean
    public ReactiveCrudRepository<ScheduledJobEntity, Integer> reactiveJobRepository(ReactiveRedisTemplate<String, Integer> keyTemplate, ReactiveRedisTemplate<String, ScheduledJobEntity> entityTemplate) {
        return new ReactiveJobRepositoryAdapter(keyTemplate, entityTemplate);
    }

    @Bean
    public ReactiveCrudRepository<ScheduledDateEntity, Integer> reactiveDateRepository(ReactiveRedisTemplate<String, Integer> keyTemplate, ReactiveRedisTemplate<String, ScheduledDateEntity> entityTemplate) {
        return new ReactiveDateRepositoryAdapter(keyTemplate, entityTemplate);
    }

    @Bean
    public ReactiveCrudRepository<ScheduledIntervalEntity, Integer> reactiveIntervalRepository(ReactiveRedisTemplate<String, Integer> keyTemplate, ReactiveRedisTemplate<String, ScheduledIntervalEntity> entityTemplate) {
        return new ReactiveIntervalRepositoryAdapter(keyTemplate, entityTemplate);
    }
}
