package hu.uni_obuda.thesis.railways.data.raildatacollector.util.adapter;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveSetOperations;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

public non-sealed abstract class CommonReactiveCrudRepositoryAdapter<T, ID> extends ReactiveCrudRepositoryAdapterBase<T, ID> {

    protected CommonReactiveCrudRepositoryAdapter(ReactiveRedisTemplate<String, ID> keyRedisTemplate, ReactiveRedisTemplate<String, T> entityRedisTemplate) {
        super(keyRedisTemplate, entityRedisTemplate);
    }

    @Override
    public abstract Mono<Boolean> existsByJobId(Integer jobId);

    @Override
    public abstract Mono<Boolean> existsByJobIdAndIntervalInMillis(Integer jobId, Long intervalInMillis);

    @Override
    public abstract Mono<Boolean> existsByJobIdAndCronExpression(Integer jobId, String cronExpression);

    @Override
    public abstract Flux<T> findByJobId(Integer jobId);

    protected abstract ID extractId(T entity);

    public Flux<ID> getKeys() {
        return keyRedisTemplate.opsForSet().members(getKeySet());
    }

    @Override
    public <S extends T> Mono<S> save(S entity) {
        return Mono.defer(() -> {
            ID id = Objects.requireNonNull(extractId(entity), "extractId(entity) returned null");
            String entityKey = entityKey(id);
            ReactiveValueOperations<String, T> values = entityRedisTemplate.opsForValue();
            ReactiveSetOperations<String, ID> ids = keyRedisTemplate.opsForSet();


            return values.set(entityKey, entity)
                    .then(ids.add(getKeySet(), id))
                    .thenReturn(entity);
        });
    }

    @Override
    public <S extends T> Flux<S> saveAll(Iterable<S> entities) {
        return Flux.fromIterable(entities).concatMap(this::save);
    }

    @Override
    public Mono<T> findById(ID id) {
        return entityRedisTemplate.opsForValue().get(entityKey(id));
    }

    @Override
    public Mono<Boolean> existsById(ID id) {
        return keyRedisTemplate.opsForSet().isMember(getKeySet(), id);
    }

    @Override
    public Flux<T> findAll() {
        return keyRedisTemplate.opsForSet()
                .members(getKeySet())
                .flatMap(this::findById);
    }

    @Override
    public Flux<T> findAllById(Iterable<ID> ids) {
        return Flux.fromIterable(ids).flatMap(this::findById);
    }

    @Override
    public Mono<Long> count() {
        return keyRedisTemplate.opsForSet().size(getKeySet());
    }

    @Override
    public Mono<Void> deleteById(ID id) {
        String eKey = entityKey(id);
        return entityRedisTemplate.delete(eKey)
                .then(keyRedisTemplate.opsForSet().remove(getKeySet(), id))
                .then();
    }

    @Override
    public Mono<Void> delete(T entity) {
        return Mono.defer(() -> deleteById(extractId(entity)));
    }

    @Override
    public Mono<Void> deleteAllById(Iterable<? extends ID> ids) {
        return Flux.fromIterable(ids).concatMap(this::deleteById).then();
    }

    @Override
    public Mono<Void> deleteAll(Iterable<? extends T> entities) {
        return Flux.fromIterable(entities).concatMap(this::delete).then();
    }

    @Override
    public Mono<Void> deleteAll() {
        return keyRedisTemplate.opsForSet()
                .members(getKeySet())
                .map(this::entityKey)
                .collectList()
                .flatMap(this::deleteAllEntityKeys)
                .then(keyRedisTemplate.delete(getKeySet()))
                .then();
    }


    protected String entityKey(ID id) {
        return getEntityPrefix() + SEPARATOR + String.valueOf(id);
    }

    private Mono<Long> deleteAllEntityKeys(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Mono.just(0L);
        }
        return entityRedisTemplate.delete(keys.toArray(new String[0]));
    }
}
