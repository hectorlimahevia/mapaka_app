package cat.mapaka.screentime;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class NfcScreenSessionController {

    private final NfcScreenSessionService service;

    public NfcScreenSessionController(NfcScreenSessionService service) {
        this.service = service;
    }

    @PostMapping("/api/screen-tags/{token}/tap")
    public ScreenSessionStatusResponse tap(@PathVariable String token) {
        return service.tap(token);
    }

    @PostMapping("/api/screen-sessions/{id}/stop")
    public ScreenSessionStatusResponse stop(@PathVariable UUID id) {
        return service.stop(id);
    }

    @PostMapping("/api/screen-sessions/{id}/assign")
    public AssignSessionResponse assign(@PathVariable UUID id, @Valid @RequestBody AssignSessionRequest request) {
        return service.assign(id, request);
    }
}
