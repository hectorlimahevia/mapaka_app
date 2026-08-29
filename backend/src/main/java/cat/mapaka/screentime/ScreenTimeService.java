package cat.mapaka.screentime;

import cat.mapaka.child.ChildProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El temps de pantalla és un monedero de minuts (Família+.pdf secció 14): el saldo és la
 * suma de tot el ledger, mai es reinicia ni s'emmagatzema com a valor fix. baseMinutes és
 * la quantitat mensual assignada al fill (ScreenTimeRule, editada en termes setmanals a
 * Fills i multiplicada per 4 — Prompt 15) — només informatiu per pintar l'anell ("de X min
 * aquest mes"), mai es genera aquí: qui crea el moviment real és
 * AllowanceGenerationService.confirm(), en el mateix acte que la paga en diners
 * (ajust posterior, Prompt 16 punt 14/24 — abans es generava diàriament, cosa que no
 * coincidia amb el que ja mostrava/editava Fills en termes mensuals).
 */
@Service
public class ScreenTimeService {

    private final ScreenTimeRuleRepository screenTimeRuleRepository;
    private final ScreenTimeTransactionRepository screenTimeTransactionRepository;

    public ScreenTimeService(
            ScreenTimeRuleRepository screenTimeRuleRepository,
            ScreenTimeTransactionRepository screenTimeTransactionRepository) {
        this.screenTimeRuleRepository = screenTimeRuleRepository;
        this.screenTimeTransactionRepository = screenTimeTransactionRepository;
    }

    @Transactional(readOnly = true)
    public ScreenTimeStatusResponse getTodayStatus(ChildProfile child) {
        int baseMinutes = screenTimeRuleRepository.findByChildIdAndWeekdayIsNullAndActiveTrue(child.getId())
                .map(ScreenTimeRule::getBaseMinutes)
                .orElse(0);
        int available = screenTimeTransactionRepository.balanceFor(child.getId());
        return new ScreenTimeStatusResponse(baseMinutes, available);
    }
}
