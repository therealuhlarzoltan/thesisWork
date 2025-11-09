package hu.uni_obuda.thesis.railways.data.delaydatacollector.controller;

import hu.uni_obuda.thesis.railways.data.common.controller.SchedulerController;
import hu.uni_obuda.thesis.railways.data.common.dto.*;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.service.scheduler.ScheduledDateService;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.service.scheduler.ScheduledIntervalService;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.service.scheduler.ScheduledJobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("scheduler")
@RequiredArgsConstructor
public class SchedulerControllerImpl implements SchedulerController {

    private final ScheduledJobService jobService;
    private final ScheduledDateService dateService;
    private final ScheduledIntervalService intervalService;

    @GetMapping("jobs")
    Flux<ScheduledJobResponse> getAllJobs() {
        return jobService.findAll();
    }

    @GetMapping("jobs/{jobId}")
    Mono<ScheduledJobResponse> getJob(@PathVariable("jobId") int id) {
        return jobService.find(id);
    }

    @PostMapping("jobs")
    Mono<ScheduledJobResponse> createJob(@Valid ScheduledJobRequest request) {
        return jobService.create(request);
    }

    @PutMapping("jobs/{jobId}")
    Mono<ScheduledJobResponse> updateJob(@PathVariable("jobId") int id, @Valid ScheduledJobRequest request) {
        return jobService.update(id, request);
    }

    @DeleteMapping("jobs/{jobId}")
    Mono<Void> deleteJob(@PathVariable("jobId") int id) {
        return jobService.delete(id);
    }

    @GetMapping("dates")
    Flux<ScheduledDateResponse> getAllDates() {
        return dateService.findAll();
    }

    @GetMapping("dates/{dateId}")
    Mono<ScheduledDateResponse> getDate(@PathVariable("dateId") int id) {
        return dateService.find(id);
    }

    @PostMapping("dates")
    Mono<ScheduledDateResponse> createDate(@Valid ScheduledDateRequest request) {
        return dateService.create(request);
    }

    @PutMapping("dates/{dateId}")
    Mono<ScheduledDateResponse> updateDate(@PathVariable("dateId") int id, @Valid ScheduledDateRequest request) {
        return dateService.update(id, request);
    }

    @DeleteMapping("dates/{jobId}")
    Mono<Void> deleteDate(@PathVariable("dateId") int id) {
        return dateService.delete(id);
    }

    @GetMapping("intervals")
    Flux<ScheduledIntervalResponse> getAllIntervals() {
        return intervalService.findAll();
    }

    @GetMapping("intervals/{intervalId}")
    Mono<ScheduledIntervalResponse> getInterval(@PathVariable("intervalId") int id) {
        return intervalService.find(id);
    }

    @PostMapping("intervals")
    Mono<ScheduledIntervalResponse> createInterval(@Valid ScheduledIntervalRequest request) {
        return intervalService.create(request);
    }

    @PutMapping("intervals/{intervalId}")
    Mono<ScheduledIntervalResponse> updateInterval(@PathVariable("intervalId") int id, @Valid ScheduledIntervalRequest request) {
        return intervalService.update(id, request);
    }

    @DeleteMapping("intervals/{intervalId}")
    Mono<Void> deleteInterval(@PathVariable("intervalId") int id) {
        return intervalService.delete(id);
    }
}
