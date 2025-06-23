package hu.uni_obuda.thesis.railways.cloud.securityserver.service;

import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public interface JsonWebTokenService {
    String generateToken(UserDetails user);
    String extractUsername(String token);
    List<String> extractRoles(String token);
    boolean validateToken(String token, UserDetails user);

    boolean validateToken(String token);
}
