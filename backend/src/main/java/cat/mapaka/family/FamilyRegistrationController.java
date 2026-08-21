package cat.mapaka.family;

import cat.mapaka.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class FamilyRegistrationController {

    private static final String REFRESH_COOKIE = "mapaka_refresh_token";

    private final FamilyRegistrationService familyRegistrationService;

    public FamilyRegistrationController(FamilyRegistrationService familyRegistrationService) {
        this.familyRegistrationService = familyRegistrationService;
    }

    @PostMapping("/api/families/register")
    public ResponseEntity<FamilyRegisterResponse> register(
            @Valid @RequestBody FamilyRegisterRequest request, HttpServletResponse response) {
        FamilyRegistrationService.RegisterResult result = familyRegistrationService.register(request);
        setRefreshCookie(response, result.refreshToken());
        return ResponseEntity.ok(result.body());
    }

    @PostMapping("/api/families/current/parents")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<Void> addParent(
            @Valid @RequestBody AddParentRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        familyRegistrationService.addParent(request, user);
        return ResponseEntity.noContent().build();
    }

    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(java.time.Duration.ofDays(30))
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
