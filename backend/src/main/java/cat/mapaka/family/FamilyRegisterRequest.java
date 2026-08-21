package cat.mapaka.family;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record FamilyRegisterRequest(
        @NotBlank String familyName,
        @NotBlank String parentDisplayName,
        @Pattern(regexp = "^\\d{4}$", message = "El PIN ha de tenir exactament 4 dígits") String parentPin) {
}
