package hu.uni_obuda.thesis.railways.data.raildatacollector.communication.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class TrainUrlResponse {
    private String trainNumber;
    private String thirdPartyUrl;
    private String officialUrl;
}
