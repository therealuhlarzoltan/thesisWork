package hu.uni_obuda.thesis.railways.cloud.securityserver.controller;

import hu.uni_obuda.thesis.railways.cloud.securityserver.dto.JwtResponse;
import hu.uni_obuda.thesis.railways.cloud.securityserver.entity.RefreshTokenEntity;
import hu.uni_obuda.thesis.railways.cloud.securityserver.repository.RefreshTokenRepository;
import hu.uni_obuda.thesis.railways.cloud.securityserver.service.JsonWebTokenService;
import hu.uni_obuda.thesis.railways.cloud.securityserver.service.RefreshTokenService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
public class RefreshControllerImpl implements RefreshController {

    private final RefreshTokenService refreshTokenService;
    private final JsonWebTokenService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    @Override
    public JwtResponse refresh(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AuthenticationCredentialsNotFoundException("Missing or invalid Authorization header");
        }

        String refreshToken = authHeader.substring(7);

        RefreshTokenEntity tokenEntity = refreshTokenRepository.findByToken(refreshToken)
                .map(refreshTokenService::verifyExpiration)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Invalid or expired refresh token"));

        String newAccessToken = jwtService.generateAccessToken(tokenEntity.getUser());
        return new JwtResponse(newAccessToken, refreshToken);
    }
}
