package hu.uni_obuda.thesis.railways.cloud.securityserver.controller;

import hu.uni_obuda.thesis.railways.cloud.securityserver.dto.JwtResponse;
import hu.uni_obuda.thesis.railways.cloud.securityserver.dto.LoginRequest;
import hu.uni_obuda.thesis.railways.cloud.securityserver.entity.RefreshTokenEntity;
import hu.uni_obuda.thesis.railways.cloud.securityserver.entity.UserEntity;
import hu.uni_obuda.thesis.railways.cloud.securityserver.repository.RefreshTokenRepository;
import hu.uni_obuda.thesis.railways.cloud.securityserver.repository.UserRepository;
import hu.uni_obuda.thesis.railways.cloud.securityserver.service.JsonWebTokenService;
import hu.uni_obuda.thesis.railways.cloud.securityserver.service.LoginService;
import hu.uni_obuda.thesis.railways.cloud.securityserver.service.RefreshTokenService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
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
