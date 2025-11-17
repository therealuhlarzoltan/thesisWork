package hu.uni_obuda.thesis.railways.data.raildatacollector.util.adapter;

import hu.uni_obuda.thesis.railways.data.raildatacollector.entity.ScheduledJobEntity;
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

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReactiveJobRepositoryAdapterTest {

    @Mock
    private ReactiveRedisTemplate<String, Integer> keyRedisTemplate;

    @Mock
    private ReactiveRedisTemplate<String, ScheduledJobEntity> entityRedisTemplate;

    @Mock
    private ReactiveSetOperations<String, Integer> setOps;

    @Mock
    private ReactiveValueOperations<String, ScheduledJobEntity> valueOps;

    private ReactiveJobRepositoryAdapter adapter;

    @BeforeEach
    public void setUp_initialiseAdapter_dependenciesInjected() {
        adapter = new ReactiveJobRepositoryAdapter(keyRedisTemplate, entityRedisTemplate);
    }

    @Test
    public void existsById_idPresent_true() {
        Integer id = 1;
        when(keyRedisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.isMember(adapter.getKeySet(), id)).thenReturn(Mono.just(true));

        Mono<Boolean> result = adapter.existsById(id);

        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();

        verify(setOps).isMember(adapter.getKeySet(), id);
    }

    @Test
    public void count_whenKeysPresent_sizeReturned() {
        when(keyRedisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.size(adapter.getKeySet())).thenReturn(Mono.just(3L));

        Mono<Long> result = adapter.count();

        StepVerifier.create(result)
                .expectNext(3L)
                .verifyComplete();

        verify(setOps).size(adapter.getKeySet());
    }

    @Test
    public void existsByJobId_jobIdPresent_true() {
        Integer jobId = 5;
        when(keyRedisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.isMember(adapter.getKeySet(), jobId)).thenReturn(Mono.just(true));

        Mono<Boolean> result = adapter.existsByJobId(jobId);

        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    public void existsByJobId_jobIdAbsent_false() {
        Integer jobId = 5;
        when(keyRedisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.isMember(adapter.getKeySet(), jobId)).thenReturn(Mono.just(false));

        Mono<Boolean> result = adapter.existsByJobId(jobId);

        StepVerifier.create(result)
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    public void findByJobId_singleJobId_entityReturned() {
        Integer jobId = 7;
        ScheduledJobEntity entity = mock(ScheduledJobEntity.class);
        when(entity.getId()).thenReturn(jobId);

        when(valueOps.get(adapter.getEntityPrefix() + ":" + jobId)).thenReturn(Mono.just(entity));

        Flux<ScheduledJobEntity> result = adapter.findByJobId(jobId);

        StepVerifier.create(result)
                .expectNext(entity)
                .verifyComplete();
    }

    @Test
    public void findAllById_multipleIds_multipleEntitiesReturned() {
        Integer id1 = 1;
        Integer id2 = 2;

        ScheduledJobEntity entity1 = mock(ScheduledJobEntity.class);
        when(entity1.getId()).thenReturn(id1);
        ScheduledJobEntity entity2 = mock(ScheduledJobEntity.class);
        when(entity2.getId()).thenReturn(id2);

        when(keyRedisTemplate.opsForSet()).thenReturn(setOps);;
        when(valueOps.get(adapter.getEntityPrefix() + ":" + id1)).thenReturn(Mono.just(entity1));
        when(valueOps.get(adapter.getEntityPrefix() + ":" + id2)).thenReturn(Mono.just(entity2));

        Flux<ScheduledJobEntity> result = adapter.findAllById(List.of(id1, id2));

        StepVerifier.create(result)
                .expectNext(entity1, entity2)
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
    public void existsByJobIdAndCronExpression_methodCalled_throwsUnsupportedOperationException() {
        Mono<Boolean> result = adapter.existsByJobIdAndCronExpression(1, "0 0 * * *");

        StepVerifier.create(result)
                .expectError(UnsupportedOperationException.class)
                .verify();
    }

    @Test
    public void saveAll_publisherOverloadCalled_throwsUnsupportedOperationException() {
        Flux<ScheduledJobEntity> publisher = Flux.empty();

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
        Flux<ScheduledJobEntity> entitiesPublisher = Flux.empty();

        StepVerifier.create(adapter.deleteAll(entitiesPublisher))
                .expectError(UnsupportedOperationException.class)
                .verify();
    }
}
