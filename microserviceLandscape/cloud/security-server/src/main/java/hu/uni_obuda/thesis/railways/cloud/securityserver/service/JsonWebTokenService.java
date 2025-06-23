package hu.uni_obuda.thesis.railways.cloud.securityserver.service;

import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public interface JsonWebTokenService {
    String generateAccessToken(UserDetails user);

    String generateRefreshToken(UserDetails user);

    String extractAccessUsername(String token);

    String extractRefreshUsername(String token);

    List<String> extractAccessRoles(String token);

    List<String> extractRefreshRoles(String token);

    boolean validateAccessToken(String token, UserDetails user);

    boolean validateRefreshToken(String token, UserDetails user);

    boolean validateAccessToken(String token);

    boolean validateRefreshToken(String token);
}
