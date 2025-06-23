package hu.uni_obuda.thesis.railways.cloud.securityserver.service;

import hu.uni_obuda.thesis.railways.cloud.securityserver.entity.RefreshTokenEntity;
import hu.uni_obuda.thesis.railways.cloud.securityserver.entity.UserEntity;
import hu.uni_obuda.thesis.railways.cloud.securityserver.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    @Value("${jwt.refresh.expiration-ms:86400000}")
    private Long refreshTokenDurationMs;

    private final JsonWebTokenService jwtService;
    private final RefreshTokenRepository tokenRepository;

    @Override
    public RefreshTokenEntity createRefreshToken(UserEntity user) {
        RefreshTokenEntity token = new RefreshTokenEntity();
        token.setUser(user);
        token.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        String jwtRefreshToken = jwtService.generateRefreshToken(user);
        token.setToken(jwtRefreshToken);
        return tokenRepository.save(token);
    }

    @Override
    public RefreshTokenEntity verifyExpiration(RefreshTokenEntity token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            tokenRepository.delete(token);
            throw new RuntimeException("Refresh token expired");
        }
        return token;
    }

    @Override
    public void invalidateToken(UserEntity user) {
        tokenRepository.deleteByUser(user);
    }
}
