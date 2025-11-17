package hu.uni_obuda.thesis.railways.data.raildatacollector.util.validator;

import hu.uni_obuda.thesis.railways.util.exception.datacollectors.InvalidInputDataException;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

public class KeyAlreadyPresentValidatorTest {

    @Test
    public void validate_existingKeysIsNull_completesWithoutError() {
        Mono<Void> result = KeyAlreadyPresentValidator.validate("key1", null);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    public void validate_existingKeysDoesNotContainKey_completesWithoutError() {
        Flux<String> existingKeys = Flux.just("key2", "key3");
        Mono<Void> result = KeyAlreadyPresentValidator.validate("key1", existingKeys);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    public void validate_existingKeysContainsKey_emitsInvalidInputDataException() {
        Flux<String> existingKeys = Flux.just("key1", "key2");
        Mono<Void> result = KeyAlreadyPresentValidator.validate("key1", existingKeys);

        StepVerifier.create(result)
                .expectErrorSatisfies(throwable -> {
                    assertInstanceOf(InvalidInputDataException.class, throwable);
                    assertEquals("Key collision, id key1 already exists, please try again", throwable.getMessage());
                })
                .verify();
    }
}
