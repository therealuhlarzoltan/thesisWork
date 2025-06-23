package hu.uni_obuda.thesis.railways.cloud.securityserver.controller;

import hu.uni_obuda.thesis.railways.cloud.securityserver.dto.JwtResponse;
import hu.uni_obuda.thesis.railways.cloud.securityserver.dto.LoginRequest;
import hu.uni_obuda.thesis.railways.cloud.securityserver.service.LoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class LoginControllerImpl implements LoginController {

    private final LoginService loginService;

    @Override
    public JwtResponse login(LoginRequest loginRequest) {
        return loginService.login(loginRequest);
    }

    @Override
    public void logout(String authHeader) {
        loginService.logout(authHeader);
    }
}
