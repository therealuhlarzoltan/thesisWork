package hu.uni_obuda.thesis.railways.data.delaydatacollector.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.*;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Configuration
public class RedisCacheConfig {

    private final ObjectMapper objectMapper;

    @Bean
    public ReactiveRedisTemplate<String, WeatherInfo> weatherInfoRedisTemplate(ReactiveRedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<WeatherInfo> jacksonSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, WeatherInfo.class);

        RedisSerializationContext<String, WeatherInfo> context = RedisSerializationContext
                .<String, WeatherInfo>newSerializationContext(new StringRedisSerializer())
                .value(jacksonSerializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

}
