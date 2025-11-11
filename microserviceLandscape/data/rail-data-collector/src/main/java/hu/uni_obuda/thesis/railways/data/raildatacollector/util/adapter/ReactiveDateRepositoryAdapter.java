package hu.uni_obuda.thesis.railways.data.raildatacollector.util.adapter;

import hu.uni_obuda.thesis.railways.data.raildatacollector.entity.ScheduledDateEntity;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

public class ReactiveDateRepositoryAdapter extends CommonReactiveCrudRepositoryAdapter<ScheduledDateEntity, Integer> {

    private static final String ENTITY_PREFIX = SCHEDULER_PREFIX + SEPARATOR + "dates";
    private static final String KEY_SET = ENTITY_PREFIX + SEPARATOR + KEYS_SUFFIX;

    public ReactiveDateRepositoryAdapter(ReactiveRedisTemplate<String, Integer> keyRedisTemplate, ReactiveRedisTemplate<String, ScheduledDateEntity> entityRedisTemplate) {
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
        return Mono.error(new UnsupportedOperationException("Not implemented"));
    }

    @Override
    public Mono<Boolean> existsByJobIdAndCronExpression(Integer jobId, String cronExpression) {
        return keyRedisTemplate.opsForSet()
                .members(getKeySet())
                .flatMap(this::findById)
                .filter(e -> Objects.equals(jobId, e.getJobId()) && Objects.equals(cronExpression, e.getCronExpression()))
                .hasElements();
    }

    @Override
    public Flux<ScheduledDateEntity> findByJobId(Integer jobId) {
        return keyRedisTemplate.opsForSet()
                .members(getKeySet())
                .flatMap(this::findById)
                .filter(e -> Objects.equals(jobId, e.getJobId()));
    }

    @Override
    protected String getEntityPrefix() {
        return ENTITY_PREFIX;
    }

    @Override
    protected String getKeySet() {
        return KEY_SET;
    }

    @Override
    protected Integer extractId(ScheduledDateEntity entity) {
        return entity.getId();
    }


}
