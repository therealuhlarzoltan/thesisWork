package hu.uni_obuda.thesis.railways.data.raildatacollector.util.adapter;

import hu.uni_obuda.thesis.railways.data.raildatacollector.entity.ScheduledDateEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveSetOperations;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReactiveDateRepositoryAdapterTest {

    @Mock
    private ReactiveRedisTemplate<String, Integer> keyRedisTemplate;
    @Mock
    private ReactiveRedisTemplate<String, ScheduledDateEntity> entityRedisTemplate;
    @Mock
    private ReactiveSetOperations<String, Integer> setOps;
    @Mock
    private ReactiveValueOperations<String, ScheduledDateEntity> valueOps;

    private ReactiveDateRepositoryAdapter testedObject;

    @BeforeEach
    void setUp() {
        testedObject = new ReactiveDateRepositoryAdapter(keyRedisTemplate, entityRedisTemplate);
        when(entityRedisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
     void save_entityWithValidId_entitySavedAndReturned() {
        ScheduledDateEntity entity = mock(ScheduledDateEntity.class);
        when(entity.getId()).thenReturn(1);

        when(keyRedisTemplate.opsForSet()).thenReturn(setOps);
        when(entityRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.set(eq(testedObject.getEntityPrefix() + ":1"), eq(entity))).thenReturn(Mono.just(true));
        when(setOps.add(eq(testedObject.getKeySet()), eq(1))).thenReturn(Mono.just(1L));

        Mono<ScheduledDateEntity> result = testedObject.save(entity);

        StepVerifier.create(result)
                .expectNext(entity)
                .verifyComplete();

        verify(valueOps).set(testedObject.getEntityPrefix() + ":1", entity);
        verify(setOps).add(testedObject.getKeySet(), 1);
    }

    @Test
     void getKeys_whenKeysPresent_fluxOfIds() {
        when(keyRedisTemplate.opsForSet()).thenReturn(setOps);
        when(entityRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(setOps.members(testedObject.getKeySet())).thenReturn(Flux.just(1, 2, 3));

        Flux<Integer> result = testedObject.getKeys();

        StepVerifier.create(result)
                .expectNext(1, 2, 3)
                .verifyComplete();

        verify(setOps).members(testedObject.getKeySet());
    }

    @Test
     void deleteAll_whenKeysPresent_allEntitiesAndKeySetDeleted() {
        when(keyRedisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.members(testedObject.getKeySet())).thenReturn(Flux.just(1, 2));
        when(entityRedisTemplate.delete(any(String[].class))).thenReturn(Mono.just(2L));
        when(keyRedisTemplate.delete(testedObject.getKeySet())).thenReturn(Mono.just(1L));

        Mono<Void> result = testedObject.deleteAll();

        StepVerifier.create(result)
                .verifyComplete();

        verify(entityRedisTemplate).delete(any(String[].class));
        verify(keyRedisTemplate).delete(testedObject.getKeySet());
    }

    @Test
     void existsByJobId_jobIdPresent_true() {
        Integer jobId = 99;
        Integer entityId = 1;

        ScheduledDateEntity entity = mock(ScheduledDateEntity.class);
        when(entity.getJobId()).thenReturn(jobId);

        when(keyRedisTemplate.opsForSet()).thenReturn(setOps);
        when(entityRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(setOps.members(testedObject.getKeySet())).thenReturn(Flux.just(entityId));
        when(valueOps.get(testedObject.getEntityPrefix() + ":" + entityId)).thenReturn(Mono.just(entity));

        Mono<Boolean> result = testedObject.existsByJobId(jobId);

        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void existsByJobId_jobIdAbsent_false() {
        Integer jobId = 99;
        Integer entityId = 1;

        ScheduledDateEntity entity = mock(ScheduledDateEntity.class);
        when(entity.getJobId()).thenReturn(100);

        when(keyRedisTemplate.opsForSet()).thenReturn(setOps);
        when(entityRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(setOps.members(testedObject.getKeySet())).thenReturn(Flux.just(entityId));
        when(valueOps.get(testedObject.getEntityPrefix() + ":" + entityId)).thenReturn(Mono.just(entity));

        Mono<Boolean> result = testedObject.existsByJobId(jobId);

        StepVerifier.create(result)
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void existsByJobIdAndCronExpression_matchingEntity_true() {
        Integer jobId = 99;
        String cron = "0 0 * * *";
        Integer entityId = 1;

        ScheduledDateEntity entity = mock(ScheduledDateEntity.class);
        when(entity.getJobId()).thenReturn(jobId);
        when(entity.getCronExpression()).thenReturn(cron);

        when(keyRedisTemplate.opsForSet()).thenReturn(setOps);
        when(entityRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(setOps.members(testedObject.getKeySet())).thenReturn(Flux.just(entityId));
        when(valueOps.get(testedObject.getEntityPrefix() + ":" + entityId)).thenReturn(Mono.just(entity));

        Mono<Boolean> result = testedObject.existsByJobIdAndCronExpression(jobId, cron);

        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void findByJobId_multipleEntitiesWithSameJobId_onlyMatchingReturned() {
        Integer jobId = 99;
        Integer id1 = 1;
        Integer id2 = 2;

        ScheduledDateEntity entity1 = mock(ScheduledDateEntity.class);
        when(entity1.getJobId()).thenReturn(jobId);

        ScheduledDateEntity entity2 = mock(ScheduledDateEntity.class);
        when(entity2.getJobId()).thenReturn(100);

        when(keyRedisTemplate.opsForSet()).thenReturn(setOps);
        when(entityRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(setOps.members(testedObject.getKeySet())).thenReturn(Flux.just(id1, id2));
        when(valueOps.get(testedObject.getEntityPrefix() + ":" + id1)).thenReturn(Mono.just(entity1));
        when(valueOps.get(testedObject.getEntityPrefix() + ":" + id2)).thenReturn(Mono.just(entity2));

        Flux<ScheduledDateEntity> result = testedObject.findByJobId(jobId);

        StepVerifier.create(result)
                .expectNext(entity1)
                .verifyComplete();
    }

    @Test
    void existsByJobIdAndIntervalInMillis_methodCalled_throwsUnsupportedOperationException() {
        Mono<Boolean> result = testedObject.existsByJobIdAndIntervalInMillis(1, 1000L);

        StepVerifier.create(result)
                .expectError(UnsupportedOperationException.class)
                .verify();
    }

    @Test
    void saveAll_publisherOverloadCalled_throwsUnsupportedOperationException() {
        Flux<ScheduledDateEntity> publisher = Flux.empty();

        StepVerifier.create(testedObject.saveAll(publisher))
                .expectError(UnsupportedOperationException.class)
                .verify();
    }

    @Test
    void existsById_publisherOverloadCalled_throwsUnsupportedOperationException() {
        Mono<Integer> idPublisher = Mono.just(1);

        StepVerifier.create(testedObject.existsById(idPublisher))
                .expectError(UnsupportedOperationException.class)
                .verify();
    }

    @Test
    void findById_publisherOverloadCalled_throwsUnsupportedOperationException() {
        Mono<Integer> idPublisher = Mono.just(1);

        StepVerifier.create(testedObject.findById(idPublisher))
                .expectError(UnsupportedOperationException.class)
                .verify();
    }

    @Test
    void findAllById_publisherOverloadCalled_throwsUnsupportedOperationException() {
        Flux<Integer> idsPublisher = Flux.just(1, 2);

        StepVerifier.create(testedObject.findAllById(idsPublisher))
                .expectError(UnsupportedOperationException.class)
                .verify();
    }

    @Test
    void deleteById_publisherOverloadCalled_throwsUnsupportedOperationException() {
        Mono<Integer> idPublisher = Mono.just(1);

        StepVerifier.create(testedObject.deleteById(idPublisher))
                .expectError(UnsupportedOperationException.class)
                .verify();
    }

    @Test
    void deleteAll_publisherOverloadCalled_throwsUnsupportedOperationException() {
        Flux<ScheduledDateEntity> entitiesPublisher = Flux.empty();

        StepVerifier.create(testedObject.deleteAll(entitiesPublisher))
                .expectError(UnsupportedOperationException.class)
                .verify();
    }
}
