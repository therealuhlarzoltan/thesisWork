package hu.uni_obuda.thesis.railways.data.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledDateRequest {
    private Integer jobId;
    private String cronExpression;
}
