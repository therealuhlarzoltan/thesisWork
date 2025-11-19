package hu.uni_obuda.thesis.railways.data.raildatacollector.communication.client;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.EmmaShortTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.EmmaShortTrainDetailsResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.EmmaTimetableResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.util.resource.CachingYamlGraphQlVariableLoader;
import hu.uni_obuda.thesis.railways.data.raildatacollector.util.resource.DefaultGraphQlVariables;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ExternalApiException;
import hu.uni_obuda.thesis.railways.util.exception.datacollectors.ExternalApiFormatMismatchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.graphql.ResponseError;
import org.springframework.graphql.client.ClientGraphQlResponse;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EmmaRailDataWebClientTest {

    private static final String BASE_URL = "https://railway.example.com";

    private static final String SHORT_TIMETABLE_DOC = "short-timetable-doc";
    private static final String LONG_TIMETABLE_DOC = "long-timetable-doc";
    private static final String SHORT_TRAIN_DETAILS_DOC = "short-train-details-doc";

    @Mock
    private HttpGraphQlClient shortTimetableClient;
    @Mock
    private HttpGraphQlClient shortTrainDetailsClient;
    @Mock
    private HttpGraphQlClient timetableClient;
    @Mock
    private CachingYamlGraphQlVariableLoader variableLoader;
    @Mock
    private HttpGraphQlClient.RequestSpec shortTimetableRequestSpec;
    @Mock
    private HttpGraphQlClient.RequestSpec shortTrainDetailsRequestSpec;
    @Mock
    private HttpGraphQlClient.RequestSpec timetableRequestSpec;
    @Mock
    private ClientGraphQlResponse shortTimetableResponse;
    @Mock
    private ClientGraphQlResponse shortTrainDetailsResponse;
    @Mock
    private ClientGraphQlResponse timetableResponse;

    private EmmaRailDataWebClient testedObject;

    @BeforeEach
    public void setUp() {
        testedObject = new EmmaRailDataWebClientImpl(
                shortTimetableClient,
                shortTrainDetailsClient,
                timetableClient,
                variableLoader
        );

        ReflectionTestUtils.setField(testedObject, "shortTimeTableDocumentName", SHORT_TIMETABLE_DOC);
        ReflectionTestUtils.setField(testedObject, "longTimeTableDocumentName", LONG_TIMETABLE_DOC);
        ReflectionTestUtils.setField(testedObject, "shortTrainDetailsDocumentName", SHORT_TRAIN_DETAILS_DOC);
        ReflectionTestUtils.setField(testedObject, "railwayBaseUrl", BASE_URL);
        ReflectionTestUtils.setField(testedObject, "timetableGetterUri", "/timetable");
        ReflectionTestUtils.setField(testedObject, "trainDetailsGetterUri", "/train-details");

        when(variableLoader.loadForDocument(SHORT_TIMETABLE_DOC))
                .thenReturn(new DefaultGraphQlVariables(Map.of("defaultKey", "defaultValue")));
        when(variableLoader.loadForDocument(LONG_TIMETABLE_DOC))
                .thenReturn(new DefaultGraphQlVariables(Map.of("defaultLongKey", "longDefault")));
        when(variableLoader.loadForDocument(SHORT_TRAIN_DETAILS_DOC))
                .thenReturn(new DefaultGraphQlVariables(Map.of("defaultDetailsKey", "detailsDefault")));
    }

    @Test
    public void getShortTimetable_whenApiReturnsValidResponse_thenMapsAndTrimsDetails() {
        LocalDate date = LocalDate.of(2024, 10, 10);
        when(shortTimetableClient.documentName(SHORT_TIMETABLE_DOC))
                .thenReturn(shortTimetableRequestSpec);
        when(shortTimetableRequestSpec.variables(anyMap()))
                .thenReturn(shortTimetableRequestSpec);
        when(shortTimetableRequestSpec.execute())
                .thenReturn(Mono.just(shortTimetableResponse));

        var railLeg = new EmmaShortTimetableResponse.Leg();
        railLeg.setMode("RAIL");
        railLeg.setStartTime(1_000L);
        railLeg.setEndTime(2_000L);

        var walkLeg = new EmmaShortTimetableResponse.Leg();
        walkLeg.setMode("WALK");
        walkLeg.setStartTime(3_000L);
        walkLeg.setEndTime(4_000L);

        var itinerary1 = new EmmaShortTimetableResponse.Itinerary();
        itinerary1.setNumberOfTransfers(1);
        itinerary1.setLegs(new ArrayList<>(List.of(railLeg, walkLeg)));

        var itinerary2 = new EmmaShortTimetableResponse.Itinerary();
        itinerary2.setNumberOfTransfers(0);
        itinerary2.setLegs(new ArrayList<>(List.of(walkLeg)));

        var plan = new EmmaShortTimetableResponse.Plan();
        plan.getItineraries().add(itinerary1);
        plan.getItineraries().add(itinerary2);

        var entity = new EmmaShortTimetableResponse();
        entity.setPlan(plan);

        when(shortTimetableResponse.isValid()).thenReturn(true);
        when(shortTimetableResponse.toEntity(EmmaShortTimetableResponse.class))
                .thenReturn(entity);

        Mono<EmmaShortTimetableResponse> result = testedObject.getShortTimetable(
                "FROM", 47.0, 19.0,
                "TO", 48.0, 20.0,
                date
        );

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getPlan()).isNotNull();
                    assertThat(response.getPlan().getItineraries()).hasSize(1);

                    EmmaShortTimetableResponse.Itinerary remaining = response.getPlan().getItineraries().getFirst();
                    assertThat(remaining.getLegs()).hasSize(1);
                    assertThat(remaining.getLegs().getFirst().getMode()).isEqualTo("RAIL");
                })
                .verifyComplete();

        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(shortTimetableRequestSpec).variables(variablesCaptor.capture());

        Map<String, Object> usedVariables = variablesCaptor.getValue();
        assertThat(usedVariables.get("defaultKey")).isEqualTo("defaultValue");
        assertThat(usedVariables.get("fromPlace")).isEqualTo("FROM::47.0,19.0");
        assertThat(usedVariables.get("toPlace")).isEqualTo("TO::48.0,20.0");
        assertThat(usedVariables.get("date")).isEqualTo("2024-10-10");
    }

    @Test
    public void getShortTimetable_whenResponseEntityParsingFails_thenEmitsExternalApiFormatMismatchException() {
        LocalDate date = LocalDate.of(2024, 10, 10);

        when(shortTimetableClient.documentName(SHORT_TIMETABLE_DOC))
                .thenReturn(shortTimetableRequestSpec);
        when(shortTimetableRequestSpec.variables(anyMap()))
                .thenReturn(shortTimetableRequestSpec);
        when(shortTimetableRequestSpec.execute())
                .thenReturn(Mono.just(shortTimetableResponse));

        when(shortTimetableResponse.isValid()).thenReturn(true);
        when(shortTimetableResponse.toEntity(EmmaShortTimetableResponse.class))
                .thenThrow(new RuntimeException("boom"));

        Mono<EmmaShortTimetableResponse> result = testedObject.getShortTimetable(
                "FROM", 47.0, 19.0,
                "TO", 48.0, 20.0,
                date
        );

        StepVerifier.create(result)
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(ExternalApiFormatMismatchException.class);
                    assertThat(throwable.getMessage()).contains("Could not parse short timetable response");
                })
                .verify();
    }

    @Test
    public void getShortTimetable_whenGraphQlResponseIsInvalid_thenEmitsExternalApiException() {
        LocalDate date = LocalDate.of(2024, 10, 10);

        when(shortTimetableClient.documentName(SHORT_TIMETABLE_DOC))
                .thenReturn(shortTimetableRequestSpec);
        when(shortTimetableRequestSpec.variables(anyMap()))
                .thenReturn(shortTimetableRequestSpec);
        when(shortTimetableRequestSpec.execute())
                .thenReturn(Mono.just(shortTimetableResponse));

        ResponseError error = mock(ResponseError.class);
        when(error.getMessage()).thenReturn("Some GraphQL error");

        when(shortTimetableResponse.isValid()).thenReturn(false);
        when(shortTimetableResponse.getErrors()).thenReturn(List.of(error));

        Mono<EmmaShortTimetableResponse> result = testedObject.getShortTimetable(
                "FROM", 47.0, 19.0,
                "TO", 48.0, 20.0,
                date
        );

        StepVerifier.create(result)
                .expectError(ExternalApiException.class)
                .verify();
    }

    @Test
    public void getShortTrainDetails_whenApiReturnsValidResponse_thenMapsResponse() {
        String trainId = "TRAIN-123";
        LocalDate serviceDate = LocalDate.of(2024, 10, 10);

        when(shortTrainDetailsClient.documentName(SHORT_TRAIN_DETAILS_DOC))
                .thenReturn(shortTrainDetailsRequestSpec);
        when(shortTrainDetailsRequestSpec.variables(anyMap()))
                .thenReturn(shortTrainDetailsRequestSpec);
        when(shortTrainDetailsRequestSpec.execute())
                .thenReturn(Mono.just(shortTrainDetailsResponse));

        var trip = new EmmaShortTrainDetailsResponse.Trip();
        trip.setId(trainId);
        EmmaShortTrainDetailsResponse entity = new EmmaShortTrainDetailsResponse();
        entity.setTrip(trip);

        when(shortTrainDetailsResponse.isValid()).thenReturn(true);
        when(shortTrainDetailsResponse.toEntity(EmmaShortTrainDetailsResponse.class))
                .thenReturn(entity);

        Mono<EmmaShortTrainDetailsResponse> result =
                testedObject.getShortTrainDetails(trainId, serviceDate);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getTrip()).isNotNull();
                    assertThat(response.getTrip().getId()).isEqualTo(trainId);
                })
                .verifyComplete();

        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(shortTrainDetailsRequestSpec).variables(variablesCaptor.capture());
        Map<String, Object> usedVariables = variablesCaptor.getValue();
        assertThat(usedVariables.get("defaultDetailsKey")).isEqualTo("detailsDefault");
        assertThat(usedVariables.get("id")).isEqualTo(trainId);
        assertThat(usedVariables.get("serviceDay")).isEqualTo("2024-10-10");
    }

    @Test
    public void getShortTrainDetails_whenResponseEntityParsingFails_thenEmitsExternalApiFormatMismatchException() {
        String trainId = "TRAIN-123";
        LocalDate serviceDate = LocalDate.of(2024, 10, 10);

        when(shortTrainDetailsClient.documentName(SHORT_TRAIN_DETAILS_DOC))
                .thenReturn(shortTrainDetailsRequestSpec);
        when(shortTrainDetailsRequestSpec.variables(anyMap()))
                .thenReturn(shortTrainDetailsRequestSpec);
        when(shortTrainDetailsRequestSpec.execute())
                .thenReturn(Mono.just(shortTrainDetailsResponse));

        when(shortTrainDetailsResponse.isValid()).thenReturn(true);
        when(shortTrainDetailsResponse.toEntity(EmmaShortTrainDetailsResponse.class))
                .thenThrow(new RuntimeException("boom"));

        Mono<EmmaShortTrainDetailsResponse> result =
                testedObject.getShortTrainDetails(trainId, serviceDate);

        StepVerifier.create(result)
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(ExternalApiFormatMismatchException.class);
                    assertThat(throwable.getMessage()).contains("Could not parse short train details response");
                })
                .verify();
    }

    @Test
    public void getShortTrainDetails_whenGraphQlResponseIsInvalid_thenEmitsExternalApiException() {
        String trainId = "TRAIN-123";
        LocalDate serviceDate = LocalDate.of(2024, 10, 10);

        when(shortTrainDetailsClient.documentName(SHORT_TRAIN_DETAILS_DOC))
                .thenReturn(shortTrainDetailsRequestSpec);
        when(shortTrainDetailsRequestSpec.variables(anyMap()))
                .thenReturn(shortTrainDetailsRequestSpec);
        when(shortTrainDetailsRequestSpec.execute())
                .thenReturn(Mono.just(shortTrainDetailsResponse));

        ResponseError error = mock(ResponseError.class);
        when(error.getMessage()).thenReturn("Some GraphQL error details");

        when(shortTrainDetailsResponse.isValid()).thenReturn(false);
        when(shortTrainDetailsResponse.getErrors()).thenReturn(List.of(error));

        Mono<EmmaShortTrainDetailsResponse> result =
                testedObject.getShortTrainDetails(trainId, serviceDate);

        StepVerifier.create(result)
                .expectError(ExternalApiException.class)
                .verify();
    }

    @Test
    public void getTimetable_whenApiReturnsValidResponse_thenMapsAndTrimsLegs() {
        LocalDate date = LocalDate.of(2024, 10, 10);

        when(timetableClient.documentName(LONG_TIMETABLE_DOC))
                .thenReturn(timetableRequestSpec);
        when(timetableRequestSpec.variables(anyMap()))
                .thenReturn(timetableRequestSpec);
        when(timetableRequestSpec.execute())
                .thenReturn(Mono.just(timetableResponse));


        var railLeg = new EmmaTimetableResponse.Leg();
        railLeg.setMode("RAIL");

        var replacementBusLeg = new EmmaTimetableResponse.Leg();
        replacementBusLeg.setMode("RAIL_REPLACEMENT_BUS");

        var walkLeg = new EmmaTimetableResponse.Leg();
        walkLeg.setMode("WALK");

        var itinerary1 = new EmmaTimetableResponse.Itinerary();
        itinerary1.setLegs(new ArrayList<>(List.of(railLeg, walkLeg, replacementBusLeg)));

        var itinerary2 = new EmmaTimetableResponse.Itinerary();
        itinerary2.setLegs(new ArrayList<>(List.of(walkLeg)));

        var plan = new EmmaTimetableResponse.Plan();
        plan.setItineraries(new ArrayList<>(List.of(itinerary1, itinerary2)));

        EmmaTimetableResponse entity = new EmmaTimetableResponse();
        entity.setPlan(plan);

        when(timetableResponse.isValid()).thenReturn(true);
        when(timetableResponse.toEntity(EmmaTimetableResponse.class))
                .thenReturn(entity);

        Mono<EmmaTimetableResponse> result =
                testedObject.getTimetable("FROM", 47.0, 19.0, "TO", 48.0, 20.0, date);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getPlan()).isNotNull();
                    assertThat(response.getPlan().getItineraries()).hasSize(1);

                    EmmaTimetableResponse.Itinerary remaining =
                            response.getPlan().getItineraries().getFirst();
                    assertThat(remaining.getLegs()).hasSize(2);
                    assertThat(remaining.getLegs().stream().map(EmmaTimetableResponse.Leg::getMode))
                            .containsExactlyInAnyOrder("RAIL", "RAIL_REPLACEMENT_BUS");
                })
                .verifyComplete();

        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(timetableRequestSpec).variables(variablesCaptor.capture());
        Map<String, Object> usedVariables = variablesCaptor.getValue();
        assertThat(usedVariables.get("defaultLongKey")).isEqualTo("longDefault");
        assertThat(usedVariables.get("fromPlace")).isEqualTo("FROM::47.0,19.0");
        assertThat(usedVariables.get("toPlace")).isEqualTo("TO::48.0,20.0");
        assertThat(usedVariables.get("date")).isEqualTo("2024-10-10");
    }

    @Test
    public void getTimetable_whenResponseEntityParsingFails_thenEmitsExternalApiFormatMismatchException() {
        LocalDate date = LocalDate.of(2024, 10, 10);

        when(timetableClient.documentName(LONG_TIMETABLE_DOC))
                .thenReturn(timetableRequestSpec);
        when(timetableRequestSpec.variables(anyMap()))
                .thenReturn(timetableRequestSpec);
        when(timetableRequestSpec.execute())
                .thenReturn(Mono.just(timetableResponse));

        when(timetableResponse.isValid()).thenReturn(true);
        when(timetableResponse.toEntity(EmmaTimetableResponse.class))
                .thenThrow(new RuntimeException("boom"));

        Mono<EmmaTimetableResponse> result =
                testedObject.getTimetable("FROM", 47.0, 19.0, "TO", 48.0, 20.0, date);

        StepVerifier.create(result)
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(ExternalApiFormatMismatchException.class);
                    assertThat(throwable.getMessage()).contains("Could not parse long timetable response");
                })
                .verify();
    }

    @Test
    public void getTimetable_whenGraphQlResponseIsInvalid_thenEmitsExternalApiException() {
        LocalDate date = LocalDate.of(2024, 10, 10);

        when(timetableClient.documentName(LONG_TIMETABLE_DOC))
                .thenReturn(timetableRequestSpec);
        when(timetableRequestSpec.variables(anyMap()))
                .thenReturn(timetableRequestSpec);
        when(timetableRequestSpec.execute())
                .thenReturn(Mono.just(timetableResponse));

        ResponseError error = mock(ResponseError.class);
        when(error.getMessage()).thenReturn("Invalid timetable request");

        when(timetableResponse.isValid()).thenReturn(false);
        when(timetableResponse.getErrors()).thenReturn(List.of(error));

        Mono<EmmaTimetableResponse> result =
                testedObject.getTimetable("FROM", 47.0, 19.0, "TO", 48.0, 20.0, date);

        StepVerifier.create(result)
                .expectError(ExternalApiException.class)
                .verify();
    }
}