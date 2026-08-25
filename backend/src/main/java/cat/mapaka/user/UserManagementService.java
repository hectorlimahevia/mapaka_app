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

    /** Autoservei: qualsevol usuari (PARENT o CHILD) pot canviar el seu propi PIN si
     * coneix l'actual — a diferència de resetPin(), aquí no cal ser PARENT perquè només
     * afecta el propi compte i exigeix demostrar que ja el coneixes. */
    @Transactional
    public void changeOwnPin(UUID userId, String oldPin, String newPin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "Usuari no trobat"));
        if (!passwordEncoder.matches(oldPin, user.getPasswordHash())) {
            throw new DomainException("INVALID_CURRENT_PIN", HttpStatus.BAD_REQUEST, "El PIN actual no és correcte");
        }
        user.setPasswordHash(passwordEncoder.encode(newPin));
        userRepository.save(user);
    }

    /** Cada usuari (PARENT o CHILD) només pot canviar el seu propi idioma (Prompt 5, i18n). */
    @Transactional
    public void updateLocale(UUID targetUserId, String locale, AuthenticatedUser actingUser) {
        if (!targetUserId.equals(actingUser.userId())) {
            throw new DomainException("ACCESS_DENIED", HttpStatus.FORBIDDEN, "Només pots canviar el teu propi idioma");
        }
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new DomainException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "Usuari no trobat"));
        target.setLocale(locale);
        userRepository.save(target);
    }
}
