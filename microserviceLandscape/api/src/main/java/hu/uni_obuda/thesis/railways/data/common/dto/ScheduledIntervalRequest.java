package hu.uni_obuda.thesis.railways.data.common.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledIntervalRequest {
    @NotNull(message = "jobId cannot be null")
    @NotBlank(message = "jobId cannot be blank")
    @Min(value = 1, message = "jobId must be grater than or equal to 1")
    private Integer jobId;

    @NotNull(message = "intervalInMillis cannot be null")
    @Min(value = 60000, message = "intervalInMillis must be at least 60000 (1 minutes) for security reasons")
    private Long intervalInMillis;
}
