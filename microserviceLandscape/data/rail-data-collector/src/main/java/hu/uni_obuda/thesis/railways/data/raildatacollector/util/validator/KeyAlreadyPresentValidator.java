package hu.uni_obuda.thesis.railways.data.raildatacollector.util.validator;

import hu.uni_obuda.thesis.railways.util.exception.datacollectors.InvalidInputDataException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public final class KeyAlreadyPresentValidator {

    private KeyAlreadyPresentValidator() {

    }

    public static <K> Mono<Void> validate(K key, Flux<K> existingKeys) {
        if (existingKeys == null) {
            return Mono.empty();
        }

        return existingKeys
                .any(existingKey -> java.util.Objects.equals(existingKey, key))
                .flatMap(found -> found
                        ? Mono.error(new InvalidInputDataException(
                        "Key collision, id " + key + " already exists, please try again"))
                        : Mono.empty());
    }
}
