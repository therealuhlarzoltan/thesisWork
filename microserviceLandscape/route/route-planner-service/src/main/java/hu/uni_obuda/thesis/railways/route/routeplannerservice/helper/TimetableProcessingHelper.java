package hu.uni_obuda.thesis.railways.route.routeplannerservice.helper;

import hu.uni_obuda.thesis.railways.data.raildatacollector.dto.TimetableResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class TimetableProcessingHelper {

    public List<List<Train>> extractRoutes(String from, String to, LocalDateTime dateTime, TimetableResponse response) {

    }

}
