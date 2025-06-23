package hu.uni_obuda.thesis.railways.cloud.securityserver.controller;

import hu.uni_obuda.thesis.railways.cloud.securityserver.dto.RegistrationRequest;
import hu.uni_obuda.thesis.railways.cloud.securityserver.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class RegistrationControllerImpl implements RegistrationController {

    private final RegistrationService registrationService;

    @Override
    public void register(@Valid RegistrationRequest registrationRequest) {
        registrationService.register(registrationRequest);
    }

    @Override
    public void registerAdmin(@Valid RegistrationRequest registrationRequest) {
        registrationService.registerAdmin(registrationRequest);
    }
}
