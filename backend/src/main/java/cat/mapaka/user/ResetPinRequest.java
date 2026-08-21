package cat.mapaka.user;

import jakarta.validation.constraints.Pattern;

public record ResetPinRequest(
        @Pattern(regexp = "^\\d{4}$", message = "El PIN ha de tenir exactament 4 dígits") String newPin) {
}
