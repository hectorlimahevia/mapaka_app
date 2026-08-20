package cat.mapaka.screentime;

import cat.mapaka.child.ChildProfile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * El temps de pantalla és un monedero de minuts (Família+.pdf secció 14): el saldo és la
 * suma de tot el ledger, mai es reinicia diàriament ni s'emmagatzema com a valor fix.
 * baseMinutes és només informatiu ("de X min assignats avui" a l'anell) — el que compta
 * és disponible = SUM(screen_time_transactions).
 */
@Service
public class ScreenTimeService {

    private final ScreenTimeRuleRepository screenTimeRuleRepository;
    private final ScreenTimeTransactionRepository screenTimeTransactionRepository;
    private final DailyBaseCreditor dailyBaseCreditor;

    public ScreenTimeService(
            ScreenTimeRuleRepository screenTimeRuleRepository,
            ScreenTimeTransactionRepository screenTimeTransactionRepository,
            DailyBaseCreditor dailyBaseCreditor) {
        this.screenTimeRuleRepository = screenTimeRuleRepository;
        this.screenTimeTransactionRepository = screenTimeTransactionRepository;
        this.dailyBaseCreditor = dailyBaseCreditor;
    }

    @Transactional
    public ScreenTimeStatusResponse getTodayStatus(ChildProfile child) {
        ZoneId familyZone = ZoneId.of(child.getUser().getFamily().getTimezone());
        LocalDate today = LocalDate.now(familyZone);

        int baseMinutes = applicableRuleFor(child.getId(), today)
                .map(ScreenTimeRule::getBaseMinutes)
                .orElse(0);

        if (baseMinutes > 0) {
            creditDailyBaseIfMissing(child, today, baseMinutes);
        }

        int available = screenTimeTransactionRepository.balanceFor(child.getId());
        return new ScreenTimeStatusResponse(baseMinutes, available);
    }

    private Optional<ScreenTimeRule> applicableRuleFor(UUID childId, LocalDate today) {
        int weekday = toWeekdayConvention(today.getDayOfWeek());
        List<ScreenTimeRule> rules = screenTimeRuleRepository.findByChildIdAndActiveTrue(childId).stream()
                .filter(r -> !r.getValidFrom().isAfter(today))
                .filter(r -> r.getValidUntil() == null || !r.getValidUntil().isBefore(today))
                .toList();

        return rules.stream().filter(r -> r.getWeekday() != null && r.getWeekday() == weekday).findFirst()
                .or(() -> rules.stream().filter(r -> r.getWeekday() == null).findFirst());
    }

    /** 0 = diumenge ... 6 = dissabte, tal com es documenta al camp weekday de la migració. */
    private int toWeekdayConvention(DayOfWeek dayOfWeek) {
        return dayOfWeek == DayOfWeek.SUNDAY ? 0 : dayOfWeek.getValue();
    }

    private void creditDailyBaseIfMissing(ChildProfile child, LocalDate today, int baseMinutes) {
        if (screenTimeTransactionRepository.existsByChildIdAndOccurredOnAndSourceType(
                child.getId(), today, ScreenSourceType.DAILY_BASE)) {
            return;
        }
        try {
            dailyBaseCreditor.credit(child, today, baseMinutes);
        } catch (DataIntegrityViolationException alreadyCreditedByAnotherRequest) {
            // Restricció idempotent (child_id, occurred_on) WHERE source_type='DAILY_BASE':
            // una altra petició gairebé simultània ja ha creat el moviment d'avui.
        }
    }
}
