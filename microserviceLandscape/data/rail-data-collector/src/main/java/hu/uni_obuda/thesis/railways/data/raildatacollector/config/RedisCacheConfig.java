package hu.uni_obuda.thesis.railways.data.raildatacollector.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.EmmaShortTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ElviraShortTimetableResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
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
}
