package hu.uni_obuda.thesis.railways.cloud.securityserver.controller;

import hu.uni_obuda.thesis.railways.cloud.securityserver.dto.JwtResponse;
import hu.uni_obuda.thesis.railways.cloud.securityserver.service.RefreshService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class RefreshControllerImpl implements RefreshController {

    private final RefreshService refreshService;

    @Override
    public JwtResponse refresh(String authHeader) {
        return refreshService.refresh(authHeader);
    }
}
