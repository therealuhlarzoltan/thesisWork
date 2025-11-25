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
public class ScheduledDateRequest {
    @NotNull(message = "jobId cannot be null")
    @Min(value = 1, message = "jobId must be grater than or equal to 1")
    private Integer jobId;

    @NotNull(message = "cronExpression cannot be null")
    @NotBlank(message = "cronExpression cannot be blank")
    private String cronExpression;
}
