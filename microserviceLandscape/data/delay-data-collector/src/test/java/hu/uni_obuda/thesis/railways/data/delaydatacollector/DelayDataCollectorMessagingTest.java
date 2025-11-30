package hu.uni_obuda.thesis.railways.data.delaydatacollector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.DelayInfoCache;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.TrainStatusCache;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.component.cache.WeatherInfoCache;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.domain.DelayEntity;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.repository.domain.DelayRepository;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.repository.domain.TrainStationRepository;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.service.data.DelayFetcherService;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.service.data.DelayService;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.service.data.GeocodingService;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.messaging.IncomingMessageSink;
import hu.uni_obuda.thesis.railways.data.event.CrudEvent;
import hu.uni_obuda.thesis.railways.data.event.HttpResponseEvent;
import hu.uni_obuda.thesis.railways.data.event.ResponsePayload;
import hu.uni_obuda.thesis.railways.data.geocodingservice.dto.GeocodingResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfoRequest;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfoRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.cloud.config.import-check.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "eureka.client.enabled=false",
                "spring.flyway.enabled=false",
                "spring.main.allow-bean-definition-overriding=true"
        }
)
@ActiveProfiles({"test", "production", "data-source-emma"})
class DelayDataCollectorMessagingTest {

    @ServiceConnection("rabbit")
    @Container
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @ServiceConnection("postgres")
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @ServiceConnection("redis")
    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @TestConfiguration
    static class TestApplicationConfig {
        @Bean
        @Qualifier("messageSenderScheduler")
        public Scheduler messageSenderScheduler() {
            return Schedulers.immediate();
        }
    }

    @MockitoBean
    GeocodingService geocodingService;

    @MockitoBean
    DelayInfoCache delayInfoCache;

    @MockitoBean
    TrainStatusCache trainStatusCache;

    @MockitoBean
    WeatherInfoCache weatherInfoCache;

    @MockitoBean
    DelayRepository delayRepository;

    @MockitoBean
    TrainStationRepository trainStationRepository;

    @Autowired
    IncomingMessageSink messageSink;

    @Autowired
    DelayService delayService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    RabbitAdmin rabbitAdmin;

    @Autowired
    DelayFetcherService delayFetcherService;

    @Autowired
    ObjectMapper objectMapper;

    private static final int MESSAGE_RESPONSE_TIMEOUT = 10;

    @DynamicPropertySource
    static void dynamicPropertySource(DynamicPropertyRegistry registry) {
        registry.add("messaging.weather.response-event.wait-duration", () -> MESSAGE_RESPONSE_TIMEOUT);
    }

    @BeforeEach
    void setUpMocks() {
        when(delayInfoCache.isDuplicate(any(DelayInfo.class))).thenReturn(Mono.just(false));
        when(delayInfoCache.cacheDelay(any(DelayInfo.class))).thenReturn(Mono.empty());

        when(trainStatusCache.markComplete(any(), any())).thenReturn(Mono.empty());
        when(trainStatusCache.markIncomplete(any(), any())).thenReturn(Mono.empty());

        when(trainStationRepository.existsById(anyString())).thenReturn(Mono.just(true));
        when(trainStationRepository.insertStation(any())).thenReturn(Mono.empty());

        when(weatherInfoCache.isCached(any(), any())).thenReturn(Mono.just(false));
        when(weatherInfoCache.retrieveWeatherInfo(any(), any()))
                .thenReturn(Mono.empty());
        when(weatherInfoCache.cacheWeatherInfo(any())).thenReturn(Mono.just(true).then());

        when(geocodingService.getCoordinatesByStation(any()))
                .thenReturn(Mono.just(
                        GeocodingResponse.builder()
                                .latitude(47.4979)
                                .longitude(19.0402)
                                .build()
                ));

        delayService.processDelays(messageSink.getDelaySink().asFlux());
    }

    @BeforeEach
    void declareAuditQueues() {
        Queue queue = new Queue("weatherDataRequests.auditGroup", true, false, false);
        rabbitAdmin.declareQueue(queue);
        TopicExchange exchange = new TopicExchange("weatherDataRequests");
        rabbitAdmin.declareExchange(exchange);
        Binding binding = BindingBuilder.bind(queue).to(exchange).with("");
        rabbitAdmin.declareBinding(binding);
    }

    @BeforeEach
    void configureRabbitTemplate() {
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
    }

    @Test
    void fetchDelay_sendsRailDataRequest_verifiedWithAuditGroup() throws JsonProcessingException {
        String trainNumber = "IC123";
        String from = "BUDAPEST";
        String to = "GYŐR";

        delayFetcherService.fetchDelay(trainNumber, from,47.4979,19.0402,
                to,47.6875,17.6504, LocalDate.now()
        );

        Message amqpMessage = rabbitTemplate.receive("railDataRequests.auditGroup", 5000);
        String json = new String(amqpMessage.getBody(), StandardCharsets.UTF_8);
        CrudEvent<String, DelayInfoRequest> event = objectMapper.readValue(json, new TypeReference<>() {});

        assertEquals(CrudEvent.Type.GET, event.getEventType());
        assertEquals(trainNumber, event.getKey());
        assertEquals(trainNumber, event.getData().getTrainNumber());
        assertEquals(from, event.getData().getFrom());
        assertEquals(to, event.getData().getTo());
    }

    @Test
    void railDelayResponse_triggersWeatherRequest_andWeatherResponse_leadsToPersist() throws JsonProcessingException {
        DelayInfo delayInfo = new DelayInfo();
        delayInfo.setTrainNumber("IC123");
        delayInfo.setStationCode("BPST");
        delayInfo.setDate(LocalDate.now());

        LocalDateTime delayTimestamp = LocalDateTime.now();
        delayInfo.setActualArrival(delayTimestamp.toString());
        delayInfo.setActualDeparture(delayTimestamp.plusMinutes(2).toString());
        delayInfo.setScheduledArrival(delayTimestamp.minusMinutes(5).toString());
        delayInfo.setScheduledDeparture(delayTimestamp.plusMinutes(1).toString());
        delayInfo.setDate(delayTimestamp.toLocalDate());

        HttpResponseEvent delayResponseEvent = new HttpResponseEvent(
                HttpResponseEvent.Type.SUCCESS,
                delayInfo.getTrainNumber(),
                new ResponsePayload(convertToJson(delayInfo), HttpStatus.OK)
        );

        rabbitTemplate.convertAndSend("railDataResponses", "", delayResponseEvent);


        Message weatherReqAmqp = rabbitTemplate.receive("weatherDataRequests.auditGroup", 5000);

        String json = new String(weatherReqAmqp.getBody(), StandardCharsets.UTF_8);
        CrudEvent<String, WeatherInfoRequest> weatherRequest = objectMapper.readValue(json, new TypeReference<>() {});


        assertThat(weatherRequest.getEventType()).isEqualTo(CrudEvent.Type.GET);
        WeatherInfoRequest weatherPayload = weatherRequest.getData();
        assertThat(weatherPayload.getStationName()).isEqualTo("BPST");

        WeatherInfo weatherInfo = new WeatherInfo();
        weatherInfo.setAddress(weatherPayload.getStationName());
        weatherInfo.setTemperature(15.0);
        weatherInfo.setWindSpeedAt10m(2.0);
        weatherInfo.setWindSpeedAt80m(4.0);
        weatherInfo.setTime(delayTimestamp);

        HttpResponseEvent weatherResponseEvent =
                new HttpResponseEvent(
                        HttpResponseEvent.Type.SUCCESS,
                        weatherRequest.getKey(),
                        new ResponsePayload(convertToJson(weatherInfo), HttpStatus.OK)
                );

        rabbitTemplate.convertAndSend("weatherDataResponses", "", weatherResponseEvent);

        Awaitility.await()
                .atMost(Duration.ofSeconds(2 * MESSAGE_RESPONSE_TIMEOUT))
                .untilAsserted(() ->
                        verify(delayRepository, atLeastOnce()).save(any(DelayEntity.class))
                );
    }

    @Test
    void railDelayResponseError_doesNotTriggerWeatherRequest_andDoesNotLoadToPersist() {
        HttpResponseEvent delayResponseEvent = new HttpResponseEvent(
                HttpResponseEvent.Type.ERROR,
                "IC123",
                new ResponsePayload(convertToJson(new RuntimeException("Bad Gateway")), HttpStatus.BAD_GATEWAY)
        );

        rabbitTemplate.convertAndSend("railDataResponses", "", delayResponseEvent);


        Message weatherReqAmqp = rabbitTemplate.receive("weatherDataRequests.auditGroup", 5000);

        Assertions.assertNull(weatherReqAmqp);

        Awaitility.await()
                .atMost(Duration.ofSeconds(2 * MESSAGE_RESPONSE_TIMEOUT))
                .untilAsserted(() -> {
                    verify(delayRepository, never()).save(any());
                });
    }

    private String convertToJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
