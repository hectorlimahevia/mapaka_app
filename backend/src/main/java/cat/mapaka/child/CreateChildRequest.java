package cat.mapaka.child;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record CreateChildRequest(
        @NotBlank String displayName,
        @NotNull LocalDate birthDate,
        String avatar,
        String colorTheme,
        @Pattern(regexp = "^\\d{4}$", message = "El PIN ha de tenir exactament 4 dígits") String pin,
        @Pattern(regexp = "^(ca|es|en)$") String locale) {
}
