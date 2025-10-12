package hu.uni_obuda.thesis.railways.route.routeplannerservice.health;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.health.StatusAggregator;

import java.util.Set;

@RequiredArgsConstructor
public class PrimaryGroupStatusAggregator implements StatusAggregator {

    private static final Status DEGRADED_STATUS = new Status("DEGRADED");

    private final StatusAggregator delegate;

    @Override
    public Status getAggregateStatus(Set<Status> statuses) {
        if (statuses.contains(Status.OUT_OF_SERVICE)) {
            return DEGRADED_STATUS;
        }
        return delegate.getAggregateStatus(statuses);
    }
}