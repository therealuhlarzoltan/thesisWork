package hu.uni_obuda.thesis.railways.cloud.securityserver.controller;

import hu.uni_obuda.thesis.railways.cloud.securityserver.dto.JwtResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

public interface RefreshController {
    @PostMapping("refresh")
    JwtResponse refresh(@RequestHeader("Authorization") String authHeader);
}
