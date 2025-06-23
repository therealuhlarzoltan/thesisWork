
package hu.uni_obuda.thesis.railways.cloud.securityserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class RegistrationRequest {

    @NotNull(message = "email.null")
    @NotBlank(message = "email.blank")
    private String email;

    @NotNull(message = "password.null")
    @NotBlank(message = "password.blank")
    @Size(min = 8, max = 64, message = "password.length")
    private String password;
}
