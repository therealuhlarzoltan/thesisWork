package hu.uni_obuda.thesis.railways.data.raildatacollector.util.adapter;

import hu.uni_obuda.thesis.railways.data.raildatacollector.entity.ScheduledJobEntity;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;


public class ReactiveJobRepositoryAdapter extends CommonReactiveCrudRepositoryAdapter<ScheduledJobEntity, Integer> {

    private static final String ENTITY_PREFIX = SCHEDULER_PREFIX + SEPARATOR + "jobs";
    private static final String KEY_SET = ENTITY_PREFIX + SEPARATOR + KEYS_SUFFIX;

    public ReactiveJobRepositoryAdapter(ReactiveRedisTemplate<String, Integer> keyRedisTemplate, ReactiveRedisTemplate<String, ScheduledJobEntity> entityRedisTemplate) {
        super(keyRedisTemplate, entityRedisTemplate);
    }

    @Override
    public Mono<Boolean> existsByJobId(Integer jobId) {
        return existsById(jobId);
    }

    @Override
    public Mono<Boolean> existsByJobIdAndIntervalInMillis(Integer jobId, Long intervalInMillis) {
        return Mono.error(new UnsupportedOperationException("Not implemented"));
    }

    @Override
    public Mono<Boolean> existsByJobIdAndCronExpression(Integer jobId, String cronExpression) {
        return Mono.error(new UnsupportedOperationException("Not implemented"));
    }

    @Override
    public Flux<ScheduledJobEntity> findByJobId(Integer jobId) {
        return findAllById(List.of(jobId));
    }

    @Override
    protected Integer extractId(ScheduledJobEntity entity) {
        return entity.getId();
    }

    @Override
    protected String getKeySet() {
        return KEY_SET;
    }

    @Override
    protected String getEntityPrefix() {
        return ENTITY_PREFIX;
    }
}
