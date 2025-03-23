package hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class TrainDelayResponse {
    private String trainNumber;
    private LocalDate date;
    private List<StationDelay> delayInfo = new ArrayList<>();
}
