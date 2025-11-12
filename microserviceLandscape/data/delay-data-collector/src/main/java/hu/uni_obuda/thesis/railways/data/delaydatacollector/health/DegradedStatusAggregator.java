package hu.uni_obuda.thesis.railways.data.delaydatacollector.health;

import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.health.StatusAggregator;

import java.util.Comparator;
import java.util.List;
import java.util.Set;


public class DegradedStatusAggregator implements StatusAggregator {

    private static final Status DEGRADED = new Status("DEGRADED");
    private static final List<Status> CUSTOM_ORDER = List.of(Status.DOWN, Status.OUT_OF_SERVICE, DEGRADED, Status.UP, Status.UNKNOWN);

    @Override
    public Status getAggregateStatus(Set<Status> statuses) {
        Status min = statuses.stream().min(StatusComparator.INSTANCE).orElse(Status.UNKNOWN);
        if (min.equals(Status.OUT_OF_SERVICE)) {
            return DEGRADED;
        }
        return min;
    }

    private static final class StatusComparator implements Comparator<Status> {

        private static final StatusComparator INSTANCE = new StatusComparator();

        @Override
        public int compare(Status s1, Status s2) {
            int index1 = CUSTOM_ORDER.indexOf(s1);
            int index2 = CUSTOM_ORDER.indexOf(s2);
            if (index1 == -1 && index2 != -1) {
                return 1;
            }
            if (index1 != -1 && index2 == -1) {
                return -1;
            }
            return Integer.compare(CUSTOM_ORDER.indexOf(s1), CUSTOM_ORDER.indexOf(s2));
        }
    }
}