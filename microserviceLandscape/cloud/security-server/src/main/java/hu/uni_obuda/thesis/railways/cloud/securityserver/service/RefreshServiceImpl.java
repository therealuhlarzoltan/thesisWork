package hu.uni_obuda.thesis.railways.cloud.securityserver.service;

import hu.uni_obuda.thesis.railways.cloud.securityserver.dto.JwtResponse;
import hu.uni_obuda.thesis.railways.cloud.securityserver.entity.RefreshTokenEntity;
import hu.uni_obuda.thesis.railways.cloud.securityserver.entity.UserEntity;
import hu.uni_obuda.thesis.railways.cloud.securityserver.repository.RefreshTokenRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class RefreshServiceImpl implements RefreshService {

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

        UserEntity owner = tokenEntity.getUser();
        if (!jwtService.validateRefreshToken(refreshToken, owner)) {
            throw new AccessDeniedException("Invalid user token");
        }

        String newAccessToken = jwtService.generateAccessToken(tokenEntity.getUser());
        return new JwtResponse(newAccessToken, refreshToken);
    }
}
