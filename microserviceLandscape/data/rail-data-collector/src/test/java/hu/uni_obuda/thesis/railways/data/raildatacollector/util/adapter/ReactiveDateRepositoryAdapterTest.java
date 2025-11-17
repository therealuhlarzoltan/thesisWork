package hu.uni_obuda.thesis.railways.data.raildatacollector.util.adapter;

import hu.uni_obuda.thesis.railways.data.raildatacollector.entity.ScheduledDateEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveSetOperations;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReactiveDateRepositoryAdapterTest {

    @Mock
    private ReactiveRedisTemplate<String, Integer> keyRedisTemplate;

    @Mock
    private ReactiveRedisTemplate<String, ScheduledDateEntity> entityRedisTemplate;

    @Mock
    private ReactiveSetOperations<String, Integer> setOps;

    @Mock
    private ReactiveValueOperations<String, ScheduledDateEntity> valueOps;

    private ReactiveDateRepositoryAdapter adapter;

    @BeforeEach
    public void setUp_initialiseAdapter_dependenciesInjected() {
        adapter = new ReactiveDateRepositoryAdapter(keyRedisTemplate, entityRedisTemplate);
    }

    @Test
    public void save_entityWithValidId_entitySavedAndReturned() {
        ScheduledDateEntity entity = mock(ScheduledDateEntity.class);
        when(entity.getId()).thenReturn(1);

        when(keyRedisTemplate.opsForSet()).thenReturn(setOps);
        when(entityRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.set(eq(adapter.getEntityPrefix() + ":1"), eq(entity))).thenReturn(Mono.just(true));
        when(setOps.add(eq(adapter.getKeySet()), eq(1))).thenReturn(Mono.just(1L));

        Mono<ScheduledDateEntity> result = adapter.save(entity);

        StepVerifier.create(result)
                .expectNext(entity)
                .verifyComplete();

        verify(valueOps).set(adapter.getEntityPrefix() + ":1", entity);
        verify(setOps).add(adapter.getKeySet(), 1);
    }

    @Test
    public void getKeys_whenKeysPresent_fluxOfIds() {
        when(keyRedisTemplate.opsForSet()).thenReturn(setOps);
        when(entityRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(setOps.members(adapter.getKeySet())).thenReturn(Flux.just(1, 2, 3));

        Flux<Integer> result = adapter.getKeys();

        StepVerifier.create(result)
                .expectNext(1, 2, 3)
                .verifyComplete();

        verify(setOps).members(adapter.getKeySet());
    }

    @Test
    public void deleteAll_whenKeysPresent_allEntitiesAndKeySetDeleted() {
        when(keyRedisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.members(adapter.getKeySet())).thenReturn(Flux.just(1, 2));
        when(entityRedisTemplate.delete(any(String[].class))).thenReturn(Mono.just(2L));
        when(keyRedisTemplate.delete(adapter.getKeySet())).thenReturn(Mono.just(1L));

        Mono<Void> result = adapter.deleteAll();

        StepVerifier.create(result)
                .verifyComplete();

        verify(entityRedisTemplate).delete(any(String[].class));
        verify(keyRedisTemplate).delete(adapter.getKeySet());
    }

    @Test
    public void existsByJobId_jobIdPresent_true() {
        Integer jobId = 99;
        Integer entityId = 1;

        ScheduledDateEntity entity = mock(ScheduledDateEntity.class);
        when(entity.getJobId()).thenReturn(jobId);

        when(keyRedisTemplate.opsForSet()).thenReturn(setOps);
        when(entityRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(setOps.members(adapter.getKeySet())).thenReturn(Flux.just(entityId));
        when(valueOps.get(adapter.getEntityPrefix() + ":" + entityId)).thenReturn(Mono.just(entity));

        Mono<Boolean> result = adapter.existsByJobId(jobId);

        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    public void existsByJobId_jobIdAbsent_false() {
        Integer jobId = 99;
        Integer entityId = 1;

        ScheduledDateEntity entity = mock(ScheduledDateEntity.class);
        when(entity.getJobId()).thenReturn(100);

        when(keyRedisTemplate.opsForSet()).thenReturn(setOps);
        when(entityRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(setOps.members(adapter.getKeySet())).thenReturn(Flux.just(entityId));
        when(valueOps.get(adapter.getEntityPrefix() + ":" + entityId)).thenReturn(Mono.just(entity));

        Mono<Boolean> result = adapter.existsByJobId(jobId);

        StepVerifier.create(result)
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    public void existsByJobIdAndCronExpression_matchingEntity_true() {
        Integer jobId = 99;
        String cron = "0 0 * * *";
        Integer entityId = 1;

        ScheduledDateEntity entity = mock(ScheduledDateEntity.class);
        when(entity.getJobId()).thenReturn(jobId);
        when(entity.getCronExpression()).thenReturn(cron);

        when(keyRedisTemplate.opsForSet()).thenReturn(setOps);
        when(entityRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(setOps.members(adapter.getKeySet())).thenReturn(Flux.just(entityId));
        when(valueOps.get(adapter.getEntityPrefix() + ":" + entityId)).thenReturn(Mono.just(entity));

        Mono<Boolean> result = adapter.existsByJobIdAndCronExpression(jobId, cron);

        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    public void findByJobId_multipleEntitiesWithSameJobId_onlyMatchingReturned() {
        Integer jobId = 99;
        Integer id1 = 1;
        Integer id2 = 2;

        ScheduledDateEntity entity1 = mock(ScheduledDateEntity.class);
        when(entity1.getJobId()).thenReturn(jobId);

        ScheduledDateEntity entity2 = mock(ScheduledDateEntity.class);
        when(entity2.getJobId()).thenReturn(100);

        when(keyRedisTemplate.opsForSet()).thenReturn(setOps);
        when(entityRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(setOps.members(adapter.getKeySet())).thenReturn(Flux.just(id1, id2));
        when(valueOps.get(adapter.getEntityPrefix() + ":" + id1)).thenReturn(Mono.just(entity1));
        when(valueOps.get(adapter.getEntityPrefix() + ":" + id2)).thenReturn(Mono.just(entity2));

        Flux<ScheduledDateEntity> result = adapter.findByJobId(jobId);

        StepVerifier.create(result)
                .expectNext(entity1)
                .verifyComplete();
    }

    @Test
    public void existsByJobIdAndIntervalInMillis_methodCalled_throwsUnsupportedOperationException() {
        Mono<Boolean> result = adapter.existsByJobIdAndIntervalInMillis(1, 1000L);

        StepVerifier.create(result)
                .expectError(UnsupportedOperationException.class)
                .verify();
    }

    @Test
    public void saveAll_publisherOverloadCalled_throwsUnsupportedOperationException() {
        Flux<ScheduledDateEntity> publisher = Flux.empty();

        StepVerifier.create(adapter.saveAll(publisher))
                .expectError(UnsupportedOperationException.class)
                .verify();
    }

    @Test
    public void existsById_publisherOverloadCalled_throwsUnsupportedOperationException() {
        Mono<Integer> idPublisher = Mono.just(1);

        StepVerifier.create(adapter.existsById(idPublisher))
                .expectError(UnsupportedOperationException.class)
                .verify();
    }

    @Test
    public void findById_publisherOverloadCalled_throwsUnsupportedOperationException() {
        Mono<Integer> idPublisher = Mono.just(1);

        StepVerifier.create(adapter.findById(idPublisher))
                .expectError(UnsupportedOperationException.class)
                .verify();
    }

    @Test
    public void findAllById_publisherOverloadCalled_throwsUnsupportedOperationException() {
        Flux<Integer> idsPublisher = Flux.just(1, 2);

        StepVerifier.create(adapter.findAllById(idsPublisher))
                .expectError(UnsupportedOperationException.class)
                .verify();
    }

    @Test
    public void deleteById_publisherOverloadCalled_throwsUnsupportedOperationException() {
        Mono<Integer> idPublisher = Mono.just(1);

        StepVerifier.create(adapter.deleteById(idPublisher))
                .expectError(UnsupportedOperationException.class)
                .verify();
    }

    @Test
    public void deleteAll_publisherOverloadCalled_throwsUnsupportedOperationException() {
        Flux<ScheduledDateEntity> entitiesPublisher = Flux.empty();

        StepVerifier.create(adapter.deleteAll(entitiesPublisher))
                .expectError(UnsupportedOperationException.class)
                .verify();
    }
}
