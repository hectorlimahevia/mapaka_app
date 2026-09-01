package cat.mapaka.screentime;

import cat.mapaka.child.ChildProfile;
import cat.mapaka.child.ChildProfileRepository;
import cat.mapaka.child.ChildSummary;
import cat.mapaka.common.DomainException;
import cat.mapaka.common.TransactionType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Sessió NFC compartida de temps de pantalla (mapaka_documento_global.md secció 6,
 * mapaka_prompts_code.md Prompts 3-4). Sense usuari autenticat: l'objecte físic (token
 * de l'etiqueta) és el que identifica la família.
 */
@Service
public class NfcScreenSessionService {

    private final ScreenTagRepository screenTagRepository;
    private final ScreenSessionRepository screenSessionRepository;
    private final ScreenSessionParticipantRepository participantRepository;
    private final ChildProfileRepository childProfileRepository;
    private final ScreenTimeTransactionRepository screenTimeTransactionRepository;

    public NfcScreenSessionService(
            ScreenTagRepository screenTagRepository,
            ScreenSessionRepository screenSessionRepository,
            ScreenSessionParticipantRepository participantRepository,
            ChildProfileRepository childProfileRepository,
            ScreenTimeTransactionRepository screenTimeTransactionRepository) {
        this.screenTagRepository = screenTagRepository;
        this.screenSessionRepository = screenSessionRepository;
        this.participantRepository = participantRepository;
        this.childProfileRepository = childProfileRepository;
        this.screenTimeTransactionRepository = screenTimeTransactionRepository;
    }

    @Transactional
    public ScreenSessionStatusResponse tap(String token) {
        ScreenTag tag = screenTagRepository.findByToken(token)
                .filter(ScreenTag::isActive)
                .orElseThrow(() -> new DomainException("SCREEN_TAG_NOT_FOUND", HttpStatus.NOT_FOUND, "Etiqueta no reconeguda"));

        return screenSessionRepository.findByScreenTagIdAndStatus(tag.getId(), ScreenSessionStatus.ACTIVE)
                .map(this::closeSession)
                .orElseGet(() -> startSession(tag));
    }

    @Transactional
    public ScreenSessionStatusResponse stop(UUID sessionId) {
        ScreenSession session = getActiveSession(sessionId);
        return closeSession(session);
    }

    @Transactional
    public AssignSessionResponse assign(UUID sessionId, AssignSessionRequest request) {
        ScreenSession session = screenSessionRepository.findById(sessionId)
                .orElseThrow(() -> new DomainException("SESSION_NOT_FOUND", HttpStatus.NOT_FOUND, "Sessió no trobada"));
        if (session.getStatus() != ScreenSessionStatus.CLOSED || session.getElapsedSeconds() == null) {
            throw new DomainException("SESSION_STILL_ACTIVE", HttpStatus.CONFLICT, "La sessió encara no s'ha aturat");
        }

        List<UUID> childIds = request.childIds();
        int totalSeconds = session.getElapsedSeconds();
        int share = totalSeconds / childIds.size();
        int remainder = totalSeconds % childIds.size();

        List<AssignSessionResponse.ParticipantResult> results = new ArrayList<>();
        for (int i = 0; i < childIds.size(); i++) {
            UUID childId = childIds.get(i);
            ChildProfile child = childProfileRepository.findById(childId)
                    .orElseThrow(() -> new DomainException("CHILD_NOT_FOUND", HttpStatus.NOT_FOUND, "Fill no trobat"));

            int assignedSeconds = share + (i == 0 ? remainder : 0);

            int minutes = assignedSeconds > 0 ? Math.max(1, Math.round(assignedSeconds / 60f)) : 0;
            if (minutes > 0) {
                screenTimeTransactionRepository.save(ScreenTimeTransaction.builder()
                        .child(child)
                        .transactionType(TransactionType.DEBIT)
                        .minutes(minutes)
                        .description("Sessió NFC compartida")
                        .sourceType(ScreenSourceType.NFC_SESSION)
                        .sourceId(null)
                        .occurredOn(LocalDate.now())
                        .createdBy(null)
                        .build());
            }

            int balance = screenTimeTransactionRepository.balanceFor(childId);
            boolean negative = balance < 0;

            participantRepository.save(ScreenSessionParticipant.builder()
                    .session(session)
                    .child(child)
                    .assignedSeconds(assignedSeconds)
                    .resultingBalanceNegative(negative)
                    .build());

            results.add(new AssignSessionResponse.ParticipantResult(
                    childId, child.getDisplayName(), assignedSeconds, balance, negative));
        }

        return new AssignSessionResponse(sessionId, results);
    }

    private ScreenSessionStatusResponse startSession(ScreenTag tag) {
        ScreenSession session = screenSessionRepository.save(ScreenSession.builder()
                .screenTag(tag)
                .startedAt(Instant.now())
                .status(ScreenSessionStatus.ACTIVE)
                .build());
        return new ScreenSessionStatusResponse(session.getId(), ScreenSessionStatus.ACTIVE, null, null);
    }

    private ScreenSessionStatusResponse closeSession(ScreenSession session) {
        Instant endedAt = Instant.now();
        int elapsedSeconds = (int) Duration.between(session.getStartedAt(), endedAt).getSeconds();
        session.setEndedAt(endedAt);
        session.setElapsedSeconds(elapsedSeconds);
        session.setStatus(ScreenSessionStatus.CLOSED);
        screenSessionRepository.save(session);

        UUID familyId = session.getScreenTag().getFamily().getId();
        List<ChildSummary> children = childProfileRepository.findAllActiveByFamilyIdFetchUser(familyId).stream()
                .map(ChildSummary::from)
                .toList();

        return new ScreenSessionStatusResponse(session.getId(), ScreenSessionStatus.CLOSED, elapsedSeconds, children);
    }

    private ScreenSession getActiveSession(UUID sessionId) {
        ScreenSession session = screenSessionRepository.findById(sessionId)
                .orElseThrow(() -> new DomainException("SESSION_NOT_FOUND", HttpStatus.NOT_FOUND, "Sessió no trobada"));
        if (session.getStatus() != ScreenSessionStatus.ACTIVE) {
            throw new DomainException("SESSION_ALREADY_CLOSED", HttpStatus.CONFLICT, "La sessió ja estava aturada");
        }
        return session;
    }
}
