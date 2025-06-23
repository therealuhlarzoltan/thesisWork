package hu.uni_obuda.thesis.railways.cloud.securityserver.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.security.Key;
import java.util.Date;
import java.util.List;


@Service
public class JsonWebTokenServiceImpl implements JsonWebTokenService {

    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.access.expiration-ms}")
    private Long accessExpirationMs;

    private Key constructSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    @Override
    public String generateToken(UserDetails user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("roles", user.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority).toList())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessExpirationMs))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    @Override
    public String extractUsername(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    @Override
    public List<String> extractRoles(String token) {
        Claims claims = parseToken(token);
        return claims.get("roles", List.class);
    }

    @Override
    public boolean validateToken(String token, UserDetails user) {
        return extractUsername(token).equals(user.getUsername());
    }

    @Override
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(constructSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
