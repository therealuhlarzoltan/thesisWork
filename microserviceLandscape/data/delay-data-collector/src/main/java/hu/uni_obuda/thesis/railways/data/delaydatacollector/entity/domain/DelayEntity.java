package hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.domain;

import hu.uni_obuda.thesis.railways.data.weatherdatacollector.dto.WeatherInfo;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("delays")
public class DelayEntity {

    @Id
    private Long id;

    @Column("station_code")
    private String stationCode; // Foreign Key

    @Column("third_party_station_url")
    private String thirdPartyStationUrl;
    @Column("official_station_url")
    private String officialStationUrl;

    @Column("train_number")
    private String trainNumber; // Foreign Key

    @Column("scheduled_departure")
    private LocalDateTime scheduledDeparture;
    @Column("actual_departure")
    private LocalDateTime actualDeparture;

    @Column("scheduled_arrival")
    private LocalDateTime scheduledArrival;
    @Column("actual_arrival")
    private LocalDateTime actualArrival;

    @Column("arrival_delay")
    private Integer arrivalDelay;
    @Column("departure_delay")
    private Integer departureDelay;

    @Column("date")
    private LocalDate date;

    @Column("weather")
    private WeatherInfo weather;
}
