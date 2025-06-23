package hu.uni_obuda.thesis.railways.cloud.securityserver.controller;

import hu.uni_obuda.thesis.railways.cloud.securityserver.dto.RegistrationRequest;
import hu.uni_obuda.thesis.railways.cloud.securityserver.entity.RoleEntity;
import hu.uni_obuda.thesis.railways.cloud.securityserver.entity.UserEntity;
import hu.uni_obuda.thesis.railways.cloud.securityserver.repository.RoleRepository;
import hu.uni_obuda.thesis.railways.cloud.securityserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@RestController
public class RegistrationControllerImpl implements RegistrationController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${role.default.name}")
    private String defaultRoleName;
    @Value("${role.admin.name}")
    private String adminRoleName;

    @Override
    public void register(RegistrationRequest registrationRequest) {
        if (userRepository.findByEmail(registrationRequest.getEmail()).isPresent()) {
            throw new BadCredentialsException("Email already in use.");
        }

        RoleEntity userRole = roleRepository.findByName(defaultRoleName)
                .orElseThrow(() -> new IllegalStateException("Default role not found"));

        UserEntity user = new UserEntity();
        user.setEmail(registrationRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registrationRequest.getPassword()));
        user.setRoles(Set.of(userRole));

        userRepository.save(user);
    }

    @Override
    public void registerAdmin(RegistrationRequest registrationRequest) {
        if (userRepository.findByEmail(registrationRequest.getEmail()).isPresent()) {
            throw new BadCredentialsException("Email already in use.");
        }

        RoleEntity adminRole = roleRepository.findByName(adminRoleName)
                .orElseThrow(() -> new IllegalStateException("Admin role not found"));

        UserEntity user = new UserEntity();
        user.setEmail(registrationRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registrationRequest.getPassword()));
        user.setRoles(Set.of(adminRole));

        userRepository.save(user);
    }
}
