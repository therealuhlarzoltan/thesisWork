package hu.uni_obuda.thesis.railways.cloud.securityserver.service;

import hu.uni_obuda.thesis.railways.cloud.securityserver.dto.RegistrationRequest;
import hu.uni_obuda.thesis.railways.cloud.securityserver.entity.RoleEntity;
import hu.uni_obuda.thesis.railways.cloud.securityserver.entity.UserEntity;
import hu.uni_obuda.thesis.railways.cloud.securityserver.repository.RoleRepository;
import hu.uni_obuda.thesis.railways.cloud.securityserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${role.default.name}")
    private String defaultRoleName;
    @Value("${role.admin.name}")
    private String adminRoleName;

    @Transactional
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

    @Transactional
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
