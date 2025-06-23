package hu.uni_obuda.thesis.railways.cloud.securityserver.service;

import hu.uni_obuda.thesis.railways.cloud.securityserver.dto.JwtResponse;
import hu.uni_obuda.thesis.railways.cloud.securityserver.dto.LoginRequest;
import org.springframework.transaction.annotation.Transactional;

public interface LoginService {
    @Transactional
    JwtResponse login(LoginRequest loginRequest);

    @Transactional
    void logout(String authHeader);
}
