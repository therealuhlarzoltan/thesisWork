package hu.uni_obuda.thesis.railways.cloud.securityserver.controller;

import hu.uni_obuda.thesis.railways.cloud.securityserver.dto.RegistrationRequest;
import hu.uni_obuda.thesis.railways.cloud.securityserver.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class RegistrationControllerImpl implements RegistrationController {

    private final RegistrationService registrationService;

    @Override
    public void register(RegistrationRequest registrationRequest) {
        registrationService.register(registrationRequest);
    }

    @Override
    public void registerAdmin(RegistrationRequest registrationRequest) {
        registrationService.registerAdmin(registrationRequest);
    }
}
