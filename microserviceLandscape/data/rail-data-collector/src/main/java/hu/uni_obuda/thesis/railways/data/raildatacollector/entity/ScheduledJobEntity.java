package hu.uni_obuda.thesis.railways.data.raildatacollector.entity;

import hu.uni_obuda.thesis.railways.util.scheduler.entity.JobEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduledJobEntity implements JobEntity {
    private Integer id;
    private String name;
}
