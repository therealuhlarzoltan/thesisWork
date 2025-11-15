package hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("trains")
public class TrainRouteEntity {
    @Id
    @Column("train_number")
    private String trainNumber;

    @Column("line_number")
    private String lineNumber;

    @Column("start_station")
    private String from;

    @Column("end_station")
    private String to;


}
