package hu.uni_obuda.thesis.railways.util.scheduler.model;

import java.time.Duration;
import java.util.Set;

public record JobScheduleDelta(
        String jobName,
        Duration fixedToAdd,
        Set<String> cronsToAdd,
        Duration fixedToRemove,
        Set<String> cronsToRemove
) {}
