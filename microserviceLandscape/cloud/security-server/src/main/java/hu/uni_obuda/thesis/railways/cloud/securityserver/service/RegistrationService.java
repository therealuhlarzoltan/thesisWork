package hu.uni_obuda.thesis.railways.cloud.securityserver.service;

import hu.uni_obuda.thesis.railways.cloud.securityserver.dto.RegistrationRequest;
import org.springframework.transaction.annotation.Transactional;

public interface RegistrationService {
    @Transactional
    void register(RegistrationRequest registrationRequest);

    @Transactional
    void registerAdmin(RegistrationRequest registrationRequest);
}
