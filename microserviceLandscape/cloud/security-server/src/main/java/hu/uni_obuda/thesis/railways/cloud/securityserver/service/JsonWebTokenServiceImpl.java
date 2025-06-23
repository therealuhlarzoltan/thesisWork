package hu.uni_obuda.thesis.railways.cloud.securityserver.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class JsonWebTokenServiceImpl extends JsonWebTokenServiceBase {

    @Override
    public String generateAccessToken(UserDetails user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("roles", user.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority).toList())
                .claim("type", "access")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessExpirationMs))
                .signWith(SignatureAlgorithm.HS256, constructSigningKey(accessSecret))
                .compact();
    }

    @Override
    public String generateRefreshToken(UserDetails user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("roles", user.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority).toList())
                .claim("type", "refresh")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(SignatureAlgorithm.HS256, constructSigningKey(refreshSecret))
                .compact();
    }

    @Override
    protected Claims parseAccessToken(String token) {
        return Jwts.parser()
                .setSigningKey(constructSigningKey(accessSecret))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    @Override
    protected Claims parseRefreshToken(String token) {
        return Jwts.parser()
                .setSigningKey(constructSigningKey(refreshSecret))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
