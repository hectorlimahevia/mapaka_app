package cat.mapaka.user;

import jakarta.validation.constraints.Pattern;

public record UpdateLocaleRequest(@Pattern(regexp = "^(ca|es|en)$") String locale) {
}
