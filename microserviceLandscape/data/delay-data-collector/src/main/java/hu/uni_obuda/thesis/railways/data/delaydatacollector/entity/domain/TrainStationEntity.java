package hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Table("stations")
public class TrainStationEntity {

    @Id
    @Column("station_code")
    private String stationCode;

    @Column("latitude")
    private Double latitude;

    @Column("longitude")
    private Double longitude;
}
