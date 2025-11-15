package hu.uni_obuda.thesis.railways.data.common.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledJobRequest {
    @NotNull(message = "jobName cannot be null")
    @NotBlank(message = "jobName cannot be blank")
    @Size(min = 1, max = 64, message = "jobName must be between 1 and 64 characters long")
    private String jobName;
}
