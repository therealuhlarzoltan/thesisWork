package hu.uni_obuda.thesis.railways.data.raildatacollector.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.GraphQlShortTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ShortTimetableResponse;
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
    public ReactiveRedisTemplate<String, ShortTimetableResponse> timetableResponseRedisTemplate(ReactiveRedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<ShortTimetableResponse> jacksonSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, ShortTimetableResponse.class);

        RedisSerializationContext<String, ShortTimetableResponse> context = RedisSerializationContext
                .<String, ShortTimetableResponse>newSerializationContext(new StringRedisSerializer())
                .value(jacksonSerializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean
    public ReactiveRedisTemplate<String, GraphQlShortTimetableResponse> graphQlTimetableResponseRedisTemplate(ReactiveRedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<GraphQlShortTimetableResponse> jacksonSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, GraphQlShortTimetableResponse.class);

        RedisSerializationContext<String, GraphQlShortTimetableResponse> context = RedisSerializationContext
                .<String, GraphQlShortTimetableResponse>newSerializationContext(new StringRedisSerializer())
                .value(jacksonSerializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}
