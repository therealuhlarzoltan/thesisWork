package hu.uni_obuda.thesis.railways.data.raildatacollector.util.adapter;

import hu.uni_obuda.thesis.railways.data.raildatacollector.entity.ScheduledIntervalEntity;
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
public class ReactiveIntervalRepositoryAdapterTest {

    @Mock
    private ReactiveRedisTemplate<String, Integer> keyRedisTemplate;

    @Mock
    private ReactiveRedisTemplate<String, ScheduledIntervalEntity> entityRedisTemplate;

    @Mock
    private ReactiveSetOperations<String, Integer> setOps;

    @Mock
    private ReactiveValueOperations<String, ScheduledIntervalEntity> valueOps;

    private ReactiveIntervalRepositoryAdapter adapter;

    @BeforeEach
    public void setUp_initialiseAdapter_dependenciesInjected() {
        adapter = new ReactiveIntervalRepositoryAdapter(keyRedisTemplate, entityRedisTemplate);
    }

    @Test
    public void findAll_whenMultipleIdsPresent_allEntitiesReturned() {
        Integer id1 = 1;
        Integer id2 = 2;

        ScheduledIntervalEntity entity1 = mock(ScheduledIntervalEntity.class);
        ScheduledIntervalEntity entity2 = mock(ScheduledIntervalEntity.class);

        when(keyRedisTemplate.opsForSet()).thenReturn(setOps);
        when(entityRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(setOps.members(adapter.getKeySet())).thenReturn(Flux.just(id1, id2));
        when(valueOps.get(adapter.getEntityPrefix() + ":" + id1)).thenReturn(Mono.just(entity1));
        when(valueOps.get(adapter.getEntityPrefix() + ":" + id2)).thenReturn(Mono.just(entity2));

        Flux<ScheduledIntervalEntity> result = adapter.findAll();

        StepVerifier.create(result)
                .expectNext(entity1, entity2)
                .verifyComplete();
    }

    @Test
    public void deleteById_existingId_entityAndKeyRemoved() {
        Integer id = 1;

        when(keyRedisTemplate.opsForSet()).thenReturn(setOps);
        when(entityRedisTemplate.delete(adapter.getEntityPrefix() + ":" + id)).thenReturn(Mono.just(1L));
        when(setOps.remove(eq(adapter.getKeySet()), any())).thenReturn(Mono.just(1L));

        Mono<Void> result = adapter.deleteById(id);

        StepVerifier.create(result)
                .verifyComplete();

        verify(entityRedisTemplate).delete(adapter.getEntityPrefix() + ":" + id);
        verify(setOps).remove(eq(adapter.getKeySet()), any());
    }

    @Test
    public void existsByJobId_jobIdPresent_true() {
        Integer jobId = 10;
        Integer entityId = 1;

        ScheduledIntervalEntity entity = mock(ScheduledIntervalEntity.class);
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
    public void existsByJobIdAndIntervalInMillis_matchingEntity_true() {
        Integer jobId = 10;
        Long interval = 5000L;
        Integer entityId = 1;

        ScheduledIntervalEntity entity = mock(ScheduledIntervalEntity.class);
        when(entity.getJobId()).thenReturn(jobId);
        when(entity.getIntervalInMillis()).thenReturn(interval);

        when(keyRedisTemplate.opsForSet()).thenReturn(setOps);
        when(entityRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(setOps.members(adapter.getKeySet())).thenReturn(Flux.just(entityId));
        when(valueOps.get(adapter.getEntityPrefix() + ":" + entityId)).thenReturn(Mono.just(entity));

        Mono<Boolean> result = adapter.existsByJobIdAndIntervalInMillis(jobId, interval);

        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    public void findByJobId_multipleIntervalsForSameJobId_allMatchingReturned() {
        Integer jobId = 10;
        Integer id1 = 1;
        Integer id2 = 2;

        ScheduledIntervalEntity entity1 = mock(ScheduledIntervalEntity.class);
        when(entity1.getJobId()).thenReturn(jobId);

        ScheduledIntervalEntity entity2 = mock(ScheduledIntervalEntity.class);
        when(entity2.getJobId()).thenReturn(jobId);

        when(keyRedisTemplate.opsForSet()).thenReturn(setOps);
        when(entityRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(setOps.members(adapter.getKeySet())).thenReturn(Flux.just(id1, id2));
        when(valueOps.get(adapter.getEntityPrefix() + ":" + id1)).thenReturn(Mono.just(entity1));
        when(valueOps.get(adapter.getEntityPrefix() + ":" + id2)).thenReturn(Mono.just(entity2));

        Flux<ScheduledIntervalEntity> result = adapter.findByJobId(jobId);

        StepVerifier.create(result)
                .expectNext(entity1, entity2)
                .verifyComplete();
    }

    @Test
    public void existsByJobIdAndCronExpression_methodCalled_throwsUnsupportedOperationException() {
        Mono<Boolean> result = adapter.existsByJobIdAndCronExpression(1, "0 0 * * *");

        StepVerifier.create(result)
                .expectError(UnsupportedOperationException.class)
                .verify();
    }

    @Test
    public void saveAll_publisherOverloadCalled_throwsUnsupportedOperationException() {
        Flux<ScheduledIntervalEntity> publisher = Flux.empty();

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
        Flux<ScheduledIntervalEntity> entitiesPublisher = Flux.empty();

        StepVerifier.create(adapter.deleteAll(entitiesPublisher))
                .expectError(UnsupportedOperationException.class)
                .verify();
    }
}
