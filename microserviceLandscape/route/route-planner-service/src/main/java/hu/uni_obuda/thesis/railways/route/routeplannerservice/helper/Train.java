package hu.uni_obuda.thesis.railways.route.routeplannerservice.helper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Train {
    private String trainNumber;
    private String lineNumber;
    private String fromStation;
    private LocalDateTime fromScheduled;
    private LocalDateTime fromActual;
    private String toStation;
    private LocalDateTime toScheduled;
    private LocalDateTime toActual;
}
