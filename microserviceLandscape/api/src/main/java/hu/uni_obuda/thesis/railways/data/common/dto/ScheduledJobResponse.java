package hu.uni_obuda.thesis.railways.data.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledJobResponse {
    private Integer id;
    private String name;
    private ScheduledIntervalResponse scheduledInterval;
    private List<ScheduledDateResponse> scheduledDates = new ArrayList<>();
}
