package cat.mapaka.user;

import cat.mapaka.common.DomainException;
import cat.mapaka.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserManagementService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Un PARENT pot resetejar el PIN de qualsevol membre (CHILD o un altre PARENT) de la
     * seva pròpia família (Prompt 6). */
    @Transactional
    public void resetPin(UUID targetUserId, String newPin, AuthenticatedUser actingUser) {
        User target = userRepository.findByIdFetchFamily(targetUserId)
                .orElseThrow(() -> new DomainException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "Usuari no trobat"));
        if (!target.getFamily().getId().equals(actingUser.familyId())) {
            throw new DomainException("ACCESS_DENIED", HttpStatus.FORBIDDEN, "No pots gestionar un usuari d'una altra família");
        }
        target.setPasswordHash(passwordEncoder.encode(newPin));
        userRepository.save(target);
    }
}
