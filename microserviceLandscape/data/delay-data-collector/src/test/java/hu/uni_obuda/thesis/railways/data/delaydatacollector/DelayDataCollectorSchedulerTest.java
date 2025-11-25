package hu.uni_obuda.thesis.railways.data.delaydatacollector;

import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledDateRequest;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledDateResponse;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledIntervalRequest;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledIntervalResponse;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledJobRequest;
import hu.uni_obuda.thesis.railways.data.common.dto.ScheduledJobResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.cloud.config.import-check.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "eureka.client.enabled=false",
        }
)
@ActiveProfiles({"test", "local-debug", "data-source-emma"})
@AutoConfigureWebTestClient
class DelayDataCollectorSchedulerTest {

    @ServiceConnection("postgres")
    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17");

    @ServiceConnection("redis")
    @Container
    static final GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine")
                    .withExposedPorts(6379);

    @Autowired
    WebTestClient webTestClient;

    private static final String BASE_PATH = "/scheduler";
    private static final int NON_EXISTENT_ID = 999_999;

    @Test
    void getAllJobs_multipleJobsCreated_returnsListContainingCreatedJobs() {
        ScheduledJobResponse job1 = createJob("getAllJobs-job-1-" + System.nanoTime());
        ScheduledJobResponse job2 = createJob("getAllJobs-job-2-" + System.nanoTime());

        List<ScheduledJobResponse> jobs = webTestClient.get()
                .uri(BASE_PATH + "/jobs")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ScheduledJobResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(jobs).isNotNull();
        assertThat(jobs)
                .extracting(ScheduledJobResponse::getName)
                .contains(job1.getName(), job2.getName());
    }

    @Test
    void getJob_existingId_returnsJobWithExpectedFields() {
        ScheduledJobResponse created = createJob("getJob-existing-" + System.nanoTime());

        ScheduledJobResponse fetched = webTestClient.get()
                .uri(BASE_PATH + "/jobs/{id}", created.getId())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ScheduledJobResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(fetched).isNotNull();
        assertThat(fetched.getId()).isEqualTo(created.getId());
        assertThat(fetched.getName()).isEqualTo(created.getName());
        assertThat(fetched.getScheduledInterval()).isNull();
        assertThat(fetched.getScheduledDates()).isNotNull();
    }

    @Test
    void getJob_nonExistingId_returnsNotFound() {
        webTestClient.get()
                .uri(BASE_PATH + "/jobs/{id}", NON_EXISTENT_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void createJob_validRequest_returnsCreatedJobWithIdAndName() {
        String jobName = "createJob-valid-" + System.nanoTime();
        ScheduledJobRequest request = new ScheduledJobRequest(jobName);

        ScheduledJobResponse response = webTestClient.post()
                .uri(BASE_PATH + "/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(ScheduledJobResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getName()).isEqualTo(jobName);
    }

    @Test
    void createJob_blankName_returnsBadRequest() {
        ScheduledJobRequest request = new ScheduledJobRequest("");

        webTestClient.post()
                .uri(BASE_PATH + "/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void createJob_nameTooLong_returnsBadRequest() {
        String longName = "x".repeat(65);
        ScheduledJobRequest request = new ScheduledJobRequest(longName);

        webTestClient.post()
                .uri(BASE_PATH + "/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void updateJob_existingId_validPayload_updatesName() {
        ScheduledJobResponse created = createJob("updateJob-before-" + System.nanoTime());
        ScheduledJobRequest updateRequest = new ScheduledJobRequest("updateJob-after-" + System.nanoTime());

        ScheduledJobResponse updated = webTestClient.put()
                .uri(BASE_PATH + "/jobs/{id}", created.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(ScheduledJobResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(updated).isNotNull();
        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getName()).isEqualTo(updateRequest.getJobName());
    }

    @Test
    void updateJob_existingId_blankName_returnsBadRequest() {
        ScheduledJobResponse created = createJob("updateJob-invalid-before-" + System.nanoTime());
        ScheduledJobRequest invalidUpdate = new ScheduledJobRequest("");

        webTestClient.put()
                .uri(BASE_PATH + "/jobs/{id}", created.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(invalidUpdate)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void updateJob_nonExistingId_returnsNotFound() {
        ScheduledJobRequest updateRequest = new ScheduledJobRequest("updateJob-nonExisting");

        webTestClient.put()
                .uri(BASE_PATH + "/jobs/{id}", NON_EXISTENT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteJob_existingId_deletesJobAndSubsequentGetReturnsNotFound() {
        ScheduledJobResponse created = createJob("deleteJob-existing-" + System.nanoTime());

        webTestClient.delete()
                .uri(BASE_PATH + "/jobs/{id}", created.getId())
                .exchange()
                .expectStatus().is2xxSuccessful();

        webTestClient.get()
                .uri(BASE_PATH + "/jobs/{id}", created.getId())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteJob_nonExistingId_returnsNotFound() {
        webTestClient.delete()
                .uri(BASE_PATH + "/jobs/{id}", NON_EXISTENT_ID)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getAllDates_multipleDatesCreated_returnsListContainingCreatedDates() {
        ScheduledJobResponse job = createJob("getAllDates-job-" + System.nanoTime());
        ScheduledDateResponse date1 = createDate(job.getId(), "0 0 * * * *");
        ScheduledDateResponse date2 = createDate(job.getId(), "0 30 * * * *");

        List<ScheduledDateResponse> dates = webTestClient.get()
                .uri(BASE_PATH + "/dates")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ScheduledDateResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(dates).isNotNull();
        assertThat(dates)
                .extracting(ScheduledDateResponse::getId)
                .contains(date1.getId(), date2.getId());
    }

    @Test
    void getDate_existingId_returnsDateWithExpectedFields() {
        ScheduledJobResponse job = createJob("getDate-job-" + System.nanoTime());
        ScheduledDateResponse created = createDate(job.getId(), "0 15 * * * *");

        ScheduledDateResponse fetched = webTestClient.get()
                .uri(BASE_PATH + "/dates/{id}", created.getId())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ScheduledDateResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(fetched).isNotNull();
        assertThat(fetched.getId()).isEqualTo(created.getId());
        assertThat(fetched.getJobId()).isEqualTo(job.getId());
        assertThat(fetched.getCronExpression()).isEqualTo(created.getCronExpression());
    }

    @Test
    void getDate_nonExistingId_returnsNotFound() {
        webTestClient.get()
                .uri(BASE_PATH + "/dates/{id}", NON_EXISTENT_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void createDate_validRequest_returnsPersistedDate() {
        ScheduledJobResponse job = createJob("createDate-job-" + System.nanoTime());
        ScheduledDateRequest request = new ScheduledDateRequest(job.getId(), "0 45 * * * *");

        ScheduledDateResponse response = webTestClient.post()
                .uri(BASE_PATH + "/dates")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(ScheduledDateResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getJobId()).isEqualTo(job.getId());
        assertThat(response.getCronExpression()).isEqualTo(request.getCronExpression());
    }

    @Test
    void createDate_invalidJobId_returnsClientError() {
        ScheduledDateRequest request = new ScheduledDateRequest(NON_EXISTENT_ID, "0 0 * * * *");

        webTestClient.post()
                .uri(BASE_PATH + "/dates")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    void createDate_blankCronExpression_returnsBadRequest() {
        ScheduledJobResponse job = createJob("createDate-blankCron-job-" + System.nanoTime());
        ScheduledDateRequest request = new ScheduledDateRequest(job.getId(), "");

        webTestClient.post()
                .uri(BASE_PATH + "/dates")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void updateDate_existingId_validPayload_updatesCronExpression() {
        ScheduledJobResponse job = createJob("updateDate-job-" + System.nanoTime());
        ScheduledDateResponse created = createDate(job.getId(), "0 0 * * * *");
        ScheduledDateRequest updateRequest = new ScheduledDateRequest(job.getId(), "0 30 * * * *");

        ScheduledDateResponse updated = webTestClient.put()
                .uri(BASE_PATH + "/dates/{id}", created.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(ScheduledDateResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(updated).isNotNull();
        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getCronExpression()).isEqualTo(updateRequest.getCronExpression());
    }

    @Test
    void updateDate_existingId_blankCronExpression_returnsBadRequest() {
        ScheduledJobResponse job = createJob("updateDate-invalid-job-" + System.nanoTime());
        ScheduledDateResponse created = createDate(job.getId(), "0 0 * * * *");
        ScheduledDateRequest invalidUpdate = new ScheduledDateRequest(job.getId(), "");

        webTestClient.put()
                .uri(BASE_PATH + "/dates/{id}", created.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(invalidUpdate)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void updateDate_nonExistingId_returnsNotFound() {
        ScheduledJobResponse job = createJob("updateDate-nonExisting-job-" + System.nanoTime());
        ScheduledDateRequest updateRequest = new ScheduledDateRequest(job.getId(), "0 0 * * * *");

        webTestClient.put()
                .uri(BASE_PATH + "/dates/{id}", NON_EXISTENT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteDate_existingId_deletesDateAndSubsequentGetReturnsNotFound() {
        ScheduledJobResponse job = createJob("deleteDate-job-" + System.nanoTime());
        ScheduledDateResponse created = createDate(job.getId(), "0 0 * * * *");

        webTestClient.delete()
                .uri(BASE_PATH + "/dates/{id}", created.getId())
                .exchange()
                .expectStatus().is2xxSuccessful();

        webTestClient.get()
                .uri(BASE_PATH + "/dates/{id}", created.getId())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteDate_nonExistingId_returnsNotFound() {
        webTestClient.delete()
                .uri(BASE_PATH + "/dates/{id}", NON_EXISTENT_ID)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getJob_afterCreatingDates_jobResponseContainsCreatedDates() {
        ScheduledJobResponse job = createJob("getJob-datesIncluded-job-" + System.nanoTime());
        ScheduledDateResponse date1 = createDate(job.getId(), "0 0 * * * *");
        ScheduledDateResponse date2 = createDate(job.getId(), "0 30 * * * *");

        ScheduledJobResponse fetched = webTestClient.get()
                .uri(BASE_PATH + "/jobs/{id}", job.getId())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ScheduledJobResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(fetched).isNotNull();
        assertThat(fetched.getScheduledDates())
                .extracting(ScheduledDateResponse::getId)
                .contains(date1.getId(), date2.getId());
    }

    @Test
    void getAllIntervals_multipleIntervalsCreated_returnsListContainingCreatedIntervals() {
        ScheduledJobResponse job1 = createJob("getAllIntervals-job-" + System.nanoTime());
        ScheduledJobResponse job2 = createJob("getAllIntervals-job-" + System.nanoTime());
        ScheduledIntervalResponse interval1 = createInterval(job1.getId(), 60_000L);
        ScheduledIntervalResponse interval2 = createInterval(job2.getId(), 120_000L);

        List<ScheduledIntervalResponse> intervals = webTestClient.get()
                .uri(BASE_PATH + "/intervals")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ScheduledIntervalResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(intervals).isNotNull();
        assertThat(intervals)
                .extracting(ScheduledIntervalResponse::getId)
                .contains(interval1.getId(), interval2.getId());
    }

    @Test
    void getInterval_existingId_returnsIntervalWithExpectedFields() {
        ScheduledJobResponse job = createJob("getInterval-job-" + System.nanoTime());
        ScheduledIntervalResponse created = createInterval(job.getId(), 120_000L);

        ScheduledIntervalResponse fetched = webTestClient.get()
                .uri(BASE_PATH + "/intervals/{id}", created.getId())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ScheduledIntervalResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(fetched).isNotNull();
        assertThat(fetched.getId()).isEqualTo(created.getId());
        assertThat(fetched.getIntervalInMillis()).isEqualTo(created.getIntervalInMillis());
    }

    @Test
    void getInterval_nonExistingId_returnsNotFound() {
        webTestClient.get()
                .uri(BASE_PATH + "/intervals/{id}", NON_EXISTENT_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void createInterval_validRequest_returnsPersistedInterval() {
        ScheduledJobResponse job = createJob("createInterval-job-" + System.nanoTime());
        ScheduledIntervalRequest request = new ScheduledIntervalRequest(job.getId(), 180_000L);

        ScheduledIntervalResponse response = webTestClient.post()
                .uri(BASE_PATH + "/intervals")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(ScheduledIntervalResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getIntervalInMillis()).isEqualTo(request.getIntervalInMillis());
    }

    @Test
    void createInterval_intervalBelowMinimum_returnsBadRequest() {
        ScheduledJobResponse job = createJob("createInterval-belowMin-job-" + System.nanoTime());
        ScheduledIntervalRequest request = new ScheduledIntervalRequest(job.getId(), 1_000L);

        webTestClient.post()
                .uri(BASE_PATH + "/intervals")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void createInterval_invalidJobId_returnsClientError() {
        ScheduledIntervalRequest request = new ScheduledIntervalRequest(NON_EXISTENT_ID, 60_000L);

        webTestClient.post()
                .uri(BASE_PATH + "/intervals")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    void updateInterval_existingId_validPayload_updatesIntervalValue() {
        ScheduledJobResponse job = createJob("updateInterval-job-" + System.nanoTime());
        ScheduledIntervalResponse created = createInterval(job.getId(), 60_000L);
        ScheduledIntervalRequest updateRequest = new ScheduledIntervalRequest(job.getId(), 300_000L);

        ScheduledIntervalResponse updated = webTestClient.put()
                .uri(BASE_PATH + "/intervals/{id}", created.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(ScheduledIntervalResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(updated).isNotNull();
        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getIntervalInMillis()).isEqualTo(updateRequest.getIntervalInMillis());
    }

    @Test
    void updateInterval_existingId_intervalBelowMinimum_returnsBadRequest() {
        ScheduledJobResponse job = createJob("updateInterval-invalid-job-" + System.nanoTime());
        ScheduledIntervalResponse created = createInterval(job.getId(), 60_000L);
        ScheduledIntervalRequest invalidUpdate = new ScheduledIntervalRequest(job.getId(), 1_000L);

        webTestClient.put()
                .uri(BASE_PATH + "/intervals/{id}", created.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(invalidUpdate)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void updateInterval_nonExistingId_returnsNotFound() {
        ScheduledJobResponse job = createJob("updateInterval-nonExisting-job-" + System.nanoTime());
        ScheduledIntervalRequest updateRequest = new ScheduledIntervalRequest(job.getId(), 300_000L);

        webTestClient.put()
                .uri(BASE_PATH + "/intervals/{id}", NON_EXISTENT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteInterval_existingId_deletesIntervalAndSubsequentGetReturnsNotFound() {
        ScheduledJobResponse job = createJob("deleteInterval-job-" + System.nanoTime());
        ScheduledIntervalResponse created = createInterval(job.getId(), 60_000L);

        webTestClient.delete()
                .uri(BASE_PATH + "/intervals/{id}", created.getId())
                .exchange()
                .expectStatus().is2xxSuccessful();

        webTestClient.get()
                .uri(BASE_PATH + "/intervals/{id}", created.getId())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteInterval_nonExistingId_returnsNotFound() {
        webTestClient.delete()
                .uri(BASE_PATH + "/intervals/{id}", NON_EXISTENT_ID)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getJob_afterCreatingInterval_jobResponseContainsInterval() {
        ScheduledJobResponse job = createJob("getJob-intervalIncluded-job-" + System.nanoTime());
        ScheduledIntervalResponse interval = createInterval(job.getId(), 90_000L);

        ScheduledJobResponse fetched = webTestClient.get()
                .uri(BASE_PATH + "/jobs/{id}", job.getId())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ScheduledJobResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(fetched).isNotNull();
        assertThat(fetched.getScheduledInterval()).isNotNull();
        assertThat(fetched.getScheduledInterval().getId()).isEqualTo(interval.getId());
        assertThat(fetched.getScheduledInterval().getIntervalInMillis())
                .isEqualTo(interval.getIntervalInMillis());
    }

    private ScheduledJobResponse createJob(String name) {
        ScheduledJobRequest request = new ScheduledJobRequest(name);

        ScheduledJobResponse response = webTestClient.post()
                .uri(BASE_PATH + "/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(ScheduledJobResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        return response;
    }

    private ScheduledDateResponse createDate(Integer jobId, String cronExpression) {
        ScheduledDateRequest request = new ScheduledDateRequest(jobId, cronExpression);

        ScheduledDateResponse response = webTestClient.post()
                .uri(BASE_PATH + "/dates")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(ScheduledDateResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        return response;
    }

    private ScheduledIntervalResponse createInterval(Integer jobId, long intervalInMillis) {
        ScheduledIntervalRequest request = new ScheduledIntervalRequest(jobId, intervalInMillis);

        ScheduledIntervalResponse response = webTestClient.post()
                .uri(BASE_PATH + "/intervals")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(ScheduledIntervalResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        return response;
    }
}
