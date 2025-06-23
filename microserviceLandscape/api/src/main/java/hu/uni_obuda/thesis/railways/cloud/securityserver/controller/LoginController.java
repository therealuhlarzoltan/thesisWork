package hu.uni_obuda.thesis.railways.cloud.securityserver.controller;

import hu.uni_obuda.thesis.railways.cloud.securityserver.dto.JwtResponse;
import hu.uni_obuda.thesis.railways.cloud.securityserver.dto.LoginRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

public interface LoginController {
    @PostMapping("login")
    JwtResponse login(@Valid @RequestBody LoginRequest loginRequest);
    @DeleteMapping("custom-logout")
    void logout(@RequestHeader("Authorization") String authHeader);
}
