package hu.uni_obuda.thesis.railways.cloud.securityserver.service;

import hu.uni_obuda.thesis.railways.cloud.securityserver.dto.JwtResponse;
import org.springframework.transaction.annotation.Transactional;

public interface RefreshService {
    @Transactional
    JwtResponse refresh(String authHeader);
}
