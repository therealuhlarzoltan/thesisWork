package hu.uni_obuda.thesis.railways.cloud.securityserver.controller;

import hu.uni_obuda.thesis.railways.cloud.securityserver.dto.RegistrationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
public class RegistrationControllerImpl implements RegistrationController {
    @Override
    public void register(RegistrationRequest registrationRequest) {

    }

    @Override
    public void registerAdmin(RegistrationRequest registrationRequest) {

    }
}
