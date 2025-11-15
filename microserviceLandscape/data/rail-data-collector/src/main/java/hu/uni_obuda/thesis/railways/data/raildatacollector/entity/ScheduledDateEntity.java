package hu.uni_obuda.thesis.railways.data.raildatacollector.entity;

import hu.uni_obuda.thesis.railways.util.scheduler.entity.CronEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduledDateEntity implements CronEntity {
    private Integer id;
    private Integer jobId;
    private String cronExpression;
}

