package hu.uni_obuda.thesis.railways.cloud.securityserver.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class LoginRequest {

    @NotNull(message = "email.null")
    @NotBlank(message = "email.blank")
    private String email;

    @NotNull(message = "password.null")
    @NotBlank(message = "password.blank")
    private String password;
}
