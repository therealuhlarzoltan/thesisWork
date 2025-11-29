package hu.uni_obuda.thesis.railways.data.delaydatacollector.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DegradedStatusAggregatorTest {

    private final DegradedStatusAggregator testedObject = new DegradedStatusAggregator();

    @Test
    void getAggregateStatus_whenContainsDown_returnsDown() {
        Set<Status> statuses = Set.of(Status.UP, Status.OUT_OF_SERVICE, Status.DOWN);

        Status result = testedObject.getAggregateStatus(statuses);

        assertThat(result).isEqualTo(Status.DOWN);
    }

    @Test
    void getAggregateStatus_whenContainsOutOfServiceButNoDown_returnsDegraded() {
        Set<Status> statuses = Set.of(Status.UP, Status.OUT_OF_SERVICE);

        Status result = testedObject.getAggregateStatus(statuses);

        assertThat(result.getCode()).isEqualTo("DEGRADED");
    }

    @Test
    void getAggregateStatus_whenOnlyUp_returnsUp() {
        Set<Status> statuses = Set.of(Status.UP);

        Status result = testedObject.getAggregateStatus(statuses);

        assertThat(result).isEqualTo(Status.UP);
    }

    @Test
    void getAggregateStatus_whenEmpty_returnsUnknown() {
        Set<Status> statuses = Set.of();

        Status result = testedObject.getAggregateStatus(statuses);

        assertThat(result).isEqualTo(Status.UNKNOWN);
    }

    @Test
    void getAggregateStatus_whenContainsDegradedAndUp_prefersDegraded() {
        Status degraded = new Status("DEGRADED");
        Set<Status> statuses = Set.of(Status.UP, degraded);

        Status result = testedObject.getAggregateStatus(statuses);

        assertThat(result.getCode()).isEqualTo("DEGRADED");
    }

    @Test
    void getAggregateStatus_whenContainsCustomAndUp_prefersUp() {
        Status custom = new Status("CUSTOM");
        Set<Status> statuses = Set.of(Status.UP, custom);

        Status result = testedObject.getAggregateStatus(statuses);

        assertThat(result).isEqualTo(Status.UP);
    }

    @Test
    void getAggregateStatus_whenOnlyCustom_returnsCustom() {
        Status custom = new Status("CUSTOM");
        Set<Status> statuses = Set.of(custom);

        Status result = testedObject.getAggregateStatus(statuses);

        assertThat(result).isEqualTo(custom);
    }
}
