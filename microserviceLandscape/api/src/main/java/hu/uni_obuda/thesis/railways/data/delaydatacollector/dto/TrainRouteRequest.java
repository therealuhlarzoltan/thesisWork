package hu.uni_obuda.thesis.railways.data.delaydatacollector.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class TrainRouteRequest {

    @NotNull(message = "Train number cannot be null")
    @NotBlank(message = "Train number cannot be blank")
    @Size(min = 1, message = "Train number must be at least 1 character long")
    @Size(max = 16, message = "Train number must be maximum 16 characters long")
    private String trainNumber;

    @NotNull(message = "Line number cannot be null")
    @NotBlank(message = "Line number cannot be blank")
    @Size(min = 1, message = "Line number must be at least 1 character long")
    @Size(max = 16, message = "Line number must be maximum 16 characters long")
    private String lineNumber;

    @NotNull(message = "Start station cannot be null")
    @NotBlank(message = "Start station cannot be blank")
    @Size(min = 2, message = "Start station must be at least 2 character long")
    @Size(max = 64, message = "Start station must be maximum 64 characters long")
    private String startStation;

    @NotNull(message = "End station cannot be null")
    @NotBlank(message = "End station cannot be blank")
    @Size(min = 2, message = "End station must be at least 2 character long")
    @Size(max = 64, message = "End station must be maximum 64 characters long")
    private String endStation;

}
