package hu.uni_obuda.thesis.railways.cloud.securityserver.service;

import hu.uni_obuda.thesis.railways.cloud.securityserver.entity.RefreshTokenEntity;
import hu.uni_obuda.thesis.railways.cloud.securityserver.entity.UserEntity;

public interface RefreshTokenService {
    RefreshTokenEntity createRefreshToken(UserEntity user);
    RefreshTokenEntity verifyExpiration(RefreshTokenEntity token);
    void invalidateToken(UserEntity user);
}
