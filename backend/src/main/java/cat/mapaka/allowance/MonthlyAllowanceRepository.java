package cat.mapaka.allowance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MonthlyAllowanceRepository extends JpaRepository<MonthlyAllowance, UUID> {

    Optional<MonthlyAllowance> findByChildIdAndYearAndMonth(UUID childId, int year, int month);

    /** Camp per a la campaneta d'alertes de Resum familiar (Prompt 15, checklist 18):
     * "ja generat aquest mes" es dedueix directament d'aquí, sense cap taula/camp nou. */
    boolean existsByChild_User_Family_IdAndYearAndMonth(UUID familyId, int year, int month);

    /** Evita LazyInitializationException quan cal comparar allowance.child.user.family.id
     * fora de transacció (controller). */
    @Query("SELECT a FROM MonthlyAllowance a JOIN FETCH a.child c JOIN FETCH c.user u JOIN FETCH u.family WHERE a.id = :id")
    Optional<MonthlyAllowance> findByIdFetchFamily(UUID id);

    /** Recupera els esborranys (DRAFT) que ja existien abans que aquesta sessió del navegador
     * arrenqués — generate() només retorna els que acaba de crear, així que sense això un
     * esborrany creat en una sessió anterior (app tancada/reinstal·lada abans de confirmar-lo)
     * queda invisible per sempre: generate() el torna a trobar ja existent i el descarta en
     * silenci, sense cap manera de tornar-lo a veure ni confirmar. */
    @Query("SELECT a FROM MonthlyAllowance a JOIN FETCH a.child c WHERE c.user.family.id = :familyId AND a.status = cat.mapaka.allowance.AllowanceStatus.DRAFT")
    List<MonthlyAllowance> findDraftsByFamilyId(UUID familyId);
}
