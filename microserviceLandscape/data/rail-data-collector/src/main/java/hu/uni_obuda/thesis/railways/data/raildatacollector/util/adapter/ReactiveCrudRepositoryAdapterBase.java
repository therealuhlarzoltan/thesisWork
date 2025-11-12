package hu.uni_obuda.thesis.railways.data.raildatacollector.util.adapter;

import org.reactivestreams.Publisher;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public sealed abstract class ReactiveCrudRepositoryAdapterBase<T, ID> implements ReactiveCrudRepositoryAdapter<T, ID> permits CommonReactiveCrudRepositoryAdapter {

    protected static final String SCHEDULER_PREFIX = ReactiveCrudRepositoryAdapter.SCHEDULER_PREFIX;
    protected static final String SEPARATOR = ":";
    protected static final String KEYS_SUFFIX = "keys";

    protected final ReactiveRedisTemplate<String, ID> keyRedisTemplate;
    protected final ReactiveRedisTemplate<String, T> entityRedisTemplate;

    protected ReactiveCrudRepositoryAdapterBase(ReactiveRedisTemplate<String, ID> keyRedisTemplate, ReactiveRedisTemplate<String, T> entityRedisTemplate) {
        this.keyRedisTemplate = keyRedisTemplate;
        this.entityRedisTemplate = entityRedisTemplate;
    }

    protected abstract String getKeySet();

    protected abstract String getEntityPrefix();

    @Override
    public <S extends T> Flux<S> saveAll(Publisher<S> entityStream) {
        return Flux.error(new UnsupportedOperationException("Not implemented"));
    }

    @Override
    public Mono<Boolean> existsById(Publisher<ID> id) {
        return Mono.error(new UnsupportedOperationException("Not implemented"));
    }

    @Override
    public Mono<T> findById(Publisher<ID> id) {
        return Mono.error(new UnsupportedOperationException("Not implemented"));
    }

    @Override
    public Flux<T> findAllById(Publisher<ID> idStream) {
        return Flux.error(new UnsupportedOperationException("Not implemented"));
    }


    @Override
    public Mono<Void> deleteById(Publisher<ID> id) {
        return Mono.error(new UnsupportedOperationException("Not implemented"));
    }


    @Override
    public Mono<Void> deleteAll(Publisher<? extends T> entityStream) {
        return Mono.error(new UnsupportedOperationException("Not implemented"));
    }
}
