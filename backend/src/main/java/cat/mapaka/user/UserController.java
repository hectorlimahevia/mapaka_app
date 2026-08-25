package cat.mapaka.user;

import cat.mapaka.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@PreAuthorize("hasRole('PARENT')")
public class UserController {

    private final UserManagementService userManagementService;

    public UserController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @PatchMapping("/api/users/{id}/pin")
    public void resetPin(
            @PathVariable UUID id, @Valid @RequestBody ResetPinRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        userManagementService.resetPin(id, request.newPin(), user);
    }

    /** Autoservei (Prompt 15): qualsevol usuari autenticat pot canviar el seu propi PIN
     * demostrant que coneix l'actual — substitueix la restricció de classe (només PARENT). */
    @PatchMapping("/api/users/me/pin")
    @PreAuthorize("isAuthenticated()")
    public void changeOwnPin(@Valid @RequestBody ChangeOwnPinRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        userManagementService.changeOwnPin(user.userId(), request.oldPin(), request.newPin());
    }

    /** Qualsevol usuari autenticat (PARENT o CHILD) pot canviar el seu propi idioma —
     * substitueix la restricció de classe (només PARENT) per a aquest mètode. */
    @PatchMapping("/api/users/{id}/locale")
    @PreAuthorize("isAuthenticated()")
    public void updateLocale(
            @PathVariable UUID id, @Valid @RequestBody UpdateLocaleRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        userManagementService.updateLocale(id, request.locale(), user);
    }
}
