package hu.uni_obuda.thesis.railways.data.common.controller;

import hu.uni_obuda.thesis.railways.data.common.dto.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SchedulerController {
    @GetMapping("jobs")
    Flux<ScheduledJobResponse> getAllJobs();

    @GetMapping("jobs/{jobId}")
    Mono<ScheduledJobResponse> getJob(@PathVariable("jobId") int id);

    @PostMapping("jobs")
    Mono<ScheduledJobResponse> createJob(@Valid ScheduledJobRequest request);

    @PutMapping("jobs/{jobId}")
    Mono<ScheduledJobResponse> updateJob(@PathVariable("jobId") int id, @Valid ScheduledJobRequest request);

    @DeleteMapping("jobs/{jobId}")
    Mono<Void> deleteJob(@PathVariable("jobId") int id);

    @GetMapping("dates")
    Flux<ScheduledDateResponse> getAllDates();

    @GetMapping("dates/{dateId}")
    Mono<ScheduledDateResponse> getDate(@PathVariable("dateId") int id);

    @PostMapping("dates")
    Mono<ScheduledDateResponse> createDate(@Valid ScheduledDateRequest request);

    @PutMapping("dates/{dateId}")
    Mono<ScheduledDateResponse> updateDate(@PathVariable("dateId") int id, @Valid ScheduledDateRequest request);

    @DeleteMapping("dates/{dateId}")
    Mono<Void> deleteDate(@PathVariable("dateId") int id);

    @GetMapping("intervals")
    Flux<ScheduledIntervalResponse> getAllIntervals();

    @GetMapping("intervals/{intervalId}")
    Mono<ScheduledIntervalResponse> getInterval(@PathVariable("intervalId") int id);

    @PostMapping("intervals")
    Mono<ScheduledIntervalResponse> createInterval(@Valid ScheduledIntervalRequest request);

    @PutMapping("intervals/{intervalId}")
    Mono<ScheduledIntervalResponse> updateInterval(@PathVariable("intervalId") int id, @Valid ScheduledIntervalRequest request);

    @DeleteMapping("intervals/{intervalId}")
    Mono<Void> deleteInterval(@PathVariable("intervalId") int id);
}
