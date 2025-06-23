package hu.uni_obuda.thesis.railways.cloud.securityserver.service;

import hu.uni_obuda.thesis.railways.cloud.securityserver.dto.JwtResponse;
import hu.uni_obuda.thesis.railways.cloud.securityserver.dto.LoginRequest;
import hu.uni_obuda.thesis.railways.cloud.securityserver.entity.RefreshTokenEntity;
import hu.uni_obuda.thesis.railways.cloud.securityserver.entity.UserEntity;
import hu.uni_obuda.thesis.railways.cloud.securityserver.repository.RefreshTokenRepository;
import hu.uni_obuda.thesis.railways.cloud.securityserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class LoginServiceImpl implements LoginService {

    private final AuthenticationManager authenticationManager;
    private final JsonWebTokenService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    @Override
    public JwtResponse login(LoginRequest loginRequest) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        UserEntity user = (UserEntity) auth.getPrincipal();

        refreshTokenRepository.deleteByUser(user);
        String jwt = jwtService.generateToken(user);
        RefreshTokenEntity refreshToken = refreshTokenService.createRefreshToken(user);

        return new JwtResponse(jwt, refreshToken.getToken());
    }

    @Transactional
    @Override
    public void logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BadCredentialsException("Missing or invalid Authorization header");
        }

        String jwt = authHeader.substring(7);

        if (!jwtService.validateToken(jwt)) {
            throw new BadCredentialsException("Invalid or expired token");
        }

        String email = jwtService.extractUsername(jwt);
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));


        boolean hasValidRole = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_USER") || role.equals("ROLE_ADMIN"));

        if (!hasValidRole) {
            throw new AccessDeniedException("User does not have permission to logout");
        }

        refreshTokenService.invalidateToken(user);
    }
}
