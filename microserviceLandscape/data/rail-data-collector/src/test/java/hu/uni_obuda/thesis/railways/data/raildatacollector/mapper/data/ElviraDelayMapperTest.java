package hu.uni_obuda.thesis.railways.data.raildatacollector.mapper.data;

import hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response.ElviraShortTrainDetailsResponse;
import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.DelayInfo;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class ElviraDelayMapperTest {

    private static final LocalDate DATE = LocalDate.of(2025, 1, 1);

    private final ElviraDelayMapper testedObject = new ElviraDelayMapper();

    private static ElviraShortTrainDetailsResponse responseOf(ElviraShortTrainDetailsResponse.Station... stations) {
        ElviraShortTrainDetailsResponse resp = new ElviraShortTrainDetailsResponse();
        List<ElviraShortTrainDetailsResponse.Station> list = new ArrayList<>(Arrays.asList(stations));
        resp.setStations(list);
        return resp;
    }

    private static ElviraShortTrainDetailsResponse.Station station(
            String code,
            String url,
            String getUrl,
            String scheduledArrival,
            String scheduledDeparture,
            String realArrival,
            String realDeparture
    ) {
        ElviraShortTrainDetailsResponse.Station s = new ElviraShortTrainDetailsResponse.Station();

        ElviraShortTrainDetailsResponse.StationDetails details =
                new ElviraShortTrainDetailsResponse.StationDetails();
        details.setCode(code);
        details.setUrl(url);
        details.setGetUrl(getUrl);

        ElviraShortTrainDetailsResponse.Schedule schedule =
                new ElviraShortTrainDetailsResponse.Schedule();
        schedule.setArrival(scheduledArrival);
        schedule.setDeparture(scheduledDeparture);

        ElviraShortTrainDetailsResponse.Real real =
                new ElviraShortTrainDetailsResponse.Real();
        real.setArrival(realArrival);
        real.setDeparture(realDeparture);

        try {
            var stationClass = ElviraShortTrainDetailsResponse.Station.class;

            var stationDetailsField = stationClass.getDeclaredField("stationDetails");
            stationDetailsField.setAccessible(true);
            stationDetailsField.set(s, details);

            var scheduleField = stationClass.getDeclaredField("schedule");
            scheduleField.setAccessible(true);
            scheduleField.set(s, schedule);

            var realField = stationClass.getDeclaredField("real");
            realField.setAccessible(true);
            realField.set(s, real);

        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to set Station nested fields via reflection", e);
        }

        return s;
    }


    @Test
    void mapToDelayInfo_returnsEmptyList_whenTrainHasNotArrived() {
        var s1 = station(
                "ST1",
                "https://official/1",
                "https://third?station=EXT1",
                null,
                "10:00",
                null,
                "10:05"
        );

        ElviraShortTrainDetailsResponse response = responseOf(s1);

        var resultMono = testedObject.mapToDelayInfo(response, "123", DATE);

        StepVerifier.create(resultMono)
                .assertNext(list -> assertThat(list).isEmpty())
                .verifyComplete();
    }

    @Test
    void mapToDelayInfo_throwsWhenFirstScheduledDepartureIsInvalid() {
        var s1 = station(
                "ST1",
                "https://official/1",
                "https://third?station=EXT1",
                "10:10",
                "invalid",
                "10:15",
                "10:20"
        );

        ElviraShortTrainDetailsResponse response = responseOf(s1);

        var resultMono = testedObject.mapToDelayInfo(response, "123", DATE);

        StepVerifier.create(resultMono)
                .expectError(java.time.format.DateTimeParseException.class)
                .verify();
    }

    @Test
    void mapToDelayInfo_mapsDelaysAndUrls_withoutRollover() {

        var s1 = station(
                "ST1",
                "https://official/1",
                "https://third?station=EXT1",
                null,
                "10:00",
                null,
                "10:05"
        );
        var s2 = station(
                "ST2",
                "https://official/2",
                "https://third?station=EXT2",
                "11:00",
                null,
                "11:10",
                null
        );

        ElviraShortTrainDetailsResponse response = responseOf(s1, s2);

        var result = testedObject.mapToDelayInfo(response, "123", DATE).block();
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);

        DelayInfo first = result.get(0);
        DelayInfo second = result.get(1);

        assertThat(first.getTrainNumber()).isEqualTo("123");
        assertThat(second.getTrainNumber()).isEqualTo("123");
        assertThat(first.getDate()).isEqualTo(DATE);
        assertThat(second.getDate()).isEqualTo(DATE);


        assertThat(first.getStationCode()).isEqualTo("ST1");
        assertThat(second.getStationCode()).isEqualTo("ST2");

        assertThat(first.getOfficialStationUrl()).isEqualTo("https://official/1");
        assertThat(second.getOfficialStationUrl()).isEqualTo("https://official/2");
        assertThat(first.getThirdPartyStationUrl()).isEqualTo("EXT1"); // after '='
        assertThat(second.getThirdPartyStationUrl()).isEqualTo("EXT2");


        assertThat(first.getScheduledDeparture())
                .isEqualTo(DATE.atTime(10, 0).toString());
        assertThat(first.getActualDeparture())
                .isEqualTo(DATE.atTime(10, 5).toString());
        assertThat(first.getDepartureDelay()).isEqualTo(5);
        assertThat(first.getScheduledArrival()).isNull();
        assertThat(first.getActualArrival()).isNull();
        assertThat(first.getArrivalDelay()).isNull();


        assertThat(second.getScheduledArrival())
                .isEqualTo(DATE.atTime(11, 0).toString());
        assertThat(second.getActualArrival())
                .isEqualTo(DATE.atTime(11, 10).toString());
        assertThat(second.getArrivalDelay()).isEqualTo(10);
        assertThat(second.getScheduledDeparture()).isNull();
        assertThat(second.getActualDeparture()).isNull();
        assertThat(second.getDepartureDelay()).isNull();
    }

    @Test
    void mapToDelayInfo_handlesMidnightRollover_forArrivalTimes() {

        var s1 = station(
                "ST1",
                "https://official/1",
                "https://third?station=EXT1",
                null,
                "23:50",
                null,
                "23:55"
        );

        var s2 = station(
                "ST2",
                "https://official/2",
                "https://third?station=EXT2",
                "00:10",
                null,
                "00:15",
                null
        );

        ElviraShortTrainDetailsResponse response = responseOf(s1, s2);

        var result = testedObject.mapToDelayInfo(response, "123", DATE).block();
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);

        DelayInfo first = result.get(0);
        DelayInfo second = result.get(1);

        assertThat(first.getScheduledDeparture())
                .isEqualTo(DATE.atTime(23, 50).toString());
        assertThat(first.getActualDeparture())
                .isEqualTo(DATE.atTime(23, 55).toString());
        assertThat(first.getDepartureDelay()).isEqualTo(5);

        LocalDate nextDay = DATE.plusDays(1);
        assertThat(second.getScheduledArrival())
                .isEqualTo(nextDay.atTime(0, 10).toString());
        assertThat(second.getActualArrival())
                .isEqualTo(nextDay.atTime(0, 15).toString());
        assertThat(second.getArrivalDelay()).isEqualTo(5);
    }

    @Test
    void mapToDelayInfo_handles24_00_asMidnightNextDay() {

        var s1 = station(
                "ST1",
                "https://official/1",
                "https://third?station=EXT1",
                null,
                "23:00",
                null,
                "23:05"
        );
        var s2 = station(
                "ST2",
                "https://official/2",
                "https://third?station=EXT2",
                "24:00",
                null,
                "00:10",
                null
        );

        ElviraShortTrainDetailsResponse response = responseOf(s1, s2);

        var result = testedObject.mapToDelayInfo(response, "123", DATE).block();
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);

        DelayInfo second = result.get(1);

        LocalDate nextDay = DATE.plusDays(1);
        assertThat(second.getScheduledArrival())
                .isEqualTo(nextDay.atTime(0, 0).toString());
        assertThat(second.getActualArrival())
                .isEqualTo(nextDay.atTime(0, 10).toString());
        assertThat(second.getArrivalDelay()).isEqualTo(10);
    }
}
