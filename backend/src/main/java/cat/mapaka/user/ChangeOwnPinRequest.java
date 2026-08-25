package cat.mapaka.user;

import jakarta.validation.constraints.Pattern;

public record ChangeOwnPinRequest(
        @Pattern(regexp = "^\\d{4}$", message = "El PIN ha de tenir exactament 4 dígits") String oldPin,
        @Pattern(regexp = "^\\d{4}$", message = "El PIN ha de tenir exactament 4 dígits") String newPin) {
}
