package hu.uni_obuda.thesis.railways.data.raildatacollector.util.adapter;

import hu.uni_obuda.thesis.railways.data.raildatacollector.entity.ScheduledIntervalEntity;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

public class ReactiveIntervalRepositoryAdapter extends CommonReactiveCrudRepositoryAdapter<ScheduledIntervalEntity, Integer> {

    private static final String ENTITY_PREFIX = SCHEDULER_PREFIX + SEPARATOR + "intervals";
    private static final String KEY_SET  = ENTITY_PREFIX + SEPARATOR + KEYS_SUFFIX;

    public ReactiveIntervalRepositoryAdapter(ReactiveRedisTemplate<String, Integer> keyRedisTemplate, ReactiveRedisTemplate<String, ScheduledIntervalEntity> entityRedisTemplate) {
        super(keyRedisTemplate, entityRedisTemplate);
    }

    @Override
    public Mono<Boolean> existsByJobId(Integer jobId) {
        return keyRedisTemplate.opsForSet()
                .members(getKeySet())
                .flatMap(this::findById)
                .filter(e -> Objects.equals(jobId, e.getJobId()))
                .hasElements();
    }

    @Override
    public Mono<Boolean> existsByJobIdAndIntervalInMillis(Integer jobId, Long intervalInMillis) {
        return keyRedisTemplate.opsForSet()
                .members(getKeySet())
                .flatMap(this::findById)
                .filter(e -> Objects.equals(jobId, e.getJobId()) && Objects.equals(intervalInMillis, e.getIntervalInMillis()))
                .hasElements();
    }

    @Override
    public Mono<Boolean> existsByJobIdAndCronExpression(Integer jobId, String cronExpression) {
        return Mono.error(new UnsupportedOperationException("Not implemented"));
    }

    @Override
    public Flux<ScheduledIntervalEntity> findByJobId(Integer jobId) {
        return keyRedisTemplate.opsForSet()
                .members(getKeySet())
                .flatMap(this::findById)
                .filter(e -> Objects.equals(jobId, e.getJobId()));
    }

    @Override
    protected String getKeySet() {
        return KEY_SET;
    }

    @Override
    protected String getEntityPrefix() {
        return ENTITY_PREFIX;
    }

    @Override
    protected Integer extractId(ScheduledIntervalEntity entity) {
        return entity.getId();
    }
}
