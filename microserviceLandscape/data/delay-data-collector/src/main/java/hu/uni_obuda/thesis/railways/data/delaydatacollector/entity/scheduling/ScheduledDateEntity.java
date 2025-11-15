package hu.uni_obuda.thesis.railways.data.delaydatacollector.entity.scheduling;

import hu.uni_obuda.thesis.railways.util.scheduler.entity.CronEntity;
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
@Table("scheduled_dates")
public class ScheduledDateEntity implements CronEntity {

    @Id
    @Column("id")
    private Integer id;

    @Column("job_id")
    private Integer jobId; // foreign key

    @Column("cron_expression")
    private String cronExpression;
}
