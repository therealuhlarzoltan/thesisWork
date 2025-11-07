package hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.scheduling;

import hu.uni_obuda.thesis.railways.util.scheduler.entity.IntervalEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("scheduled_intervals")
public class ScheduledIntervalEntity implements IntervalEntity {

    @Id
    @Column("id")
    private Integer id;

    @Column("job_id")
    private Integer jobId; // foreign key

    @Column("interval_in_millis")
    private Long intervalInMillis;
}
