package hu.uni_obuda.thesis.railways.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class DelayPredictionResponse {
    private String trainNumber;
    private String stationCode;
    private Double predictedDelay;
}
