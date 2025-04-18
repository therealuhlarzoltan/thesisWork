package hu.uni_obuda.thesis.railways.data.raildatacollector.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class DelayInfoRequest {
    private String trainNumber;
    private String from;
    private String to;
    private LocalDate date;
}
