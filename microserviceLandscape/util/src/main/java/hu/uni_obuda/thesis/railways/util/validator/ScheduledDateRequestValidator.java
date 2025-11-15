package hu.uni_obuda.thesis.railways.util.validator;

import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledDateRequest;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.InvalidInputDataException;
import org.springframework.scheduling.support.CronExpression;
import reactor.core.publisher.Mono;

public class ScheduledDateRequestValidator {

    public static Mono<ScheduledDateRequest> validate(ScheduledDateRequest scheduledDateRequest) {
        if (!CronExpression.isValidExpression(scheduledDateRequest.getCronExpression())) {
            return Mono.error(new InvalidInputDataException("cronExpression is invalid"));
        }

        return Mono.just(scheduledDateRequest);
    }
}
