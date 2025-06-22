package hu.uni_obuda.thesis.railways.cloud.securityserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class RefreshTokenResponse {
    private String refreshToken;
}
