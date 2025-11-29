package hu.uni_obuda.thesis.railways.data.delaydatacollector.controller;

import hu.uni_obuda.thesis.railways.data.common.controller.SchedulerController;
import hu.uni_obuda.thesis.railways.data.common.dto.*;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.service.scheduler.ScheduledDateService;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.service.scheduler.ScheduledIntervalService;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.service.scheduler.ScheduledJobService;
import hu.uni_obuda.thesis.railways.util.validator.ScheduledDateRequestValidator;
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

    @Override
    public Flux<ScheduledJobResponse> getAllJobs() {
        return jobService.findAll();
    }

    @Override
    public Mono<ScheduledJobResponse> getJob(@PathVariable("jobId") int id) {
        return jobService.find(id);
    }

    @Override
    public Mono<ScheduledJobResponse> createJob(@Valid @RequestBody ScheduledJobRequest request) {
        return jobService.create(request);
    }

    @Override
    public Mono<ScheduledJobResponse> updateJob(@PathVariable("jobId") int id, @Valid @RequestBody ScheduledJobRequest request) {
        return jobService.update(id, request);
    }

    @Override
    public Mono<Void> deleteJob(@PathVariable("jobId") int id) {
        return jobService.delete(id);
    }

    @Override
    public Flux<ScheduledDateResponse> getAllDates() {
        return dateService.findAll();
    }

    @Override
    public Mono<ScheduledDateResponse> getDate(@PathVariable("dateId") int id) {
        return dateService.find(id);
    }

    @Override
    public Mono<ScheduledDateResponse> createDate(@Valid @RequestBody ScheduledDateRequest request) {
        return ScheduledDateRequestValidator.validate(request).flatMap(dateService::create);
    }

    @Override
    public Mono<ScheduledDateResponse> updateDate(@PathVariable("dateId") int id, @Valid @RequestBody ScheduledDateRequest request) {
        return ScheduledDateRequestValidator.validate(request).flatMap(date -> dateService.update(id, date));
    }

    @Override
    public Mono<Void> deleteDate(@PathVariable("dateId") int id) {
        return dateService.delete(id);
    }

    @Override
    public Flux<ScheduledIntervalResponse> getAllIntervals() {
        return intervalService.findAll();
    }

    @Override
    public Mono<ScheduledIntervalResponse> getInterval(@PathVariable("intervalId") int id) {
        return intervalService.find(id);
    }

    @Override
    public Mono<ScheduledIntervalResponse> createInterval(@Valid ScheduledIntervalRequest request) {
        return intervalService.create(request);
    }

    @Override
    public Mono<ScheduledIntervalResponse> updateInterval(@PathVariable("intervalId") int id, @Valid @RequestBody ScheduledIntervalRequest request) {
        return intervalService.update(id, request);
    }

    @Override
    public Mono<Void> deleteInterval(@PathVariable("intervalId") int id) {
        return intervalService.delete(id);
    }
}
