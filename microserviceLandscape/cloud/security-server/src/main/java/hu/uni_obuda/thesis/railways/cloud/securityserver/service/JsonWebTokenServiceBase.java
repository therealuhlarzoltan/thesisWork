package hu.uni_obuda.thesis.railways.cloud.securityserver.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Key;
import java.util.List;

@Slf4j
public abstract class JsonWebTokenServiceBase implements JsonWebTokenService {

    @Value("${jwt.access.secret}")
    protected String accessSecret;
    @Value("${jwt.access.expiration-ms}")
    protected Long accessExpirationMs;
    @Value("${jwt.refresh.secret}")
    protected String refreshSecret;
    @Value("${jwt.refresh.expiration-ms}")
    protected Long refreshExpirationMs;

    protected Key constructSigningKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    protected abstract Claims parseAccessToken(String token);

    protected abstract Claims parseRefreshToken(String token);

    @Override
    public abstract String generateAccessToken(UserDetails user);

    @Override
    public abstract String generateRefreshToken(UserDetails user);

    @Override
    public String extractAccessUsername(String token) {
        return parseAccessToken(token).getSubject();
    }

    @Override
    public String extractRefreshUsername(String token) {
        return parseRefreshToken(token).getSubject();
    }

    @Override
    public List<String> extractAccessRoles(String token) {
        return parseAccessToken(token).get("roles", List.class);
    }

    @Override
    public List<String> extractRefreshRoles(String token) {
        return parseRefreshToken(token).get("roles", List.class);
    }

    @Override
    public boolean validateAccessToken(String token, UserDetails user) {
        return extractAccessUsername(token).equals(user.getUsername()) && isAccessToken(token);
    }

    @Override
    public boolean validateRefreshToken(String token, UserDetails user) {
        return extractRefreshUsername(token).equals(user.getUsername()) &&isRefreshToken(token);
    }

    @Override
    public boolean validateAccessToken(String token) {
        try {
            return isAccessToken(token);
        } catch (Exception e) {
            log.warn("Invalid token: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean validateRefreshToken(String token) {
        try {
            return isRefreshToken(token);
        } catch (Exception e) {
            log.warn("Invalid token: {}", e.getMessage());
            return false;
        }
    }

    protected boolean isAccessToken(String token) {
        try {
            return "access".equals(parseAccessToken(token).get("type"));
        } catch (Exception e) {
            log.warn("Invalid token: {}", e.getMessage());
            return false;
        }
    }

    protected boolean isRefreshToken(String token) {
        try {
            return "refresh".equals(parseRefreshToken(token).get("type"));
        } catch (Exception e) {
            log.warn("Invalid token: {}", e.getMessage());
            return false;
        }
    }
}
