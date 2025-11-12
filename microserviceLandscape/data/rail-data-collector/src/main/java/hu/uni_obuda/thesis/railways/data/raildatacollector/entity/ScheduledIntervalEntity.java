package hu.uni_obuda.thesis.railways.data.raildatacollector.entity;

import hu.uni_obuda.thesis.railways.util.scheduler.entity.IntervalEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduledIntervalEntity implements IntervalEntity {
    private Integer id;
    private Integer jobId;
    private Long intervalInMillis;
}

