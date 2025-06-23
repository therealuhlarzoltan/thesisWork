package hu.uni_obuda.thesis.railways.cloud.securityserver.worker;

import hu.uni_obuda.thesis.railways.cloud.securityserver.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
@Service
public class TokenCleanupService {

    private final RefreshTokenRepository tokenRepository;

    @Scheduled(cron = "0 0 0 * * *")
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();
        int deleted = tokenRepository.deleteByExpiryDateBefore(now);
        log.info("Deleted {} expired refresh tokens at {}", deleted, now);
    }
}
