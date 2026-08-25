package cat.mapaka.task;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.WeekFields;
import java.util.Locale;

/** Determina si un moment concret cau dins el mateix període de recurrència que "ara"
 * (o un altre instant de referència) — compartit entre TaskService (fill) i les tasques
 * incompletes de PARENT (Prompt 15) perquè cap dels dos repliqui aquest càlcul. */
public final class RecurrenceWindow {

    private static final WeekFields WEEK_FIELDS = WeekFields.of(Locale.forLanguageTag("ca"));

    private RecurrenceWindow() {
    }

    public static boolean isWithinCurrentPeriod(Instant completedAt, RecurrenceType recurrenceType, ZoneId familyZone) {
        return isWithinCurrentPeriod(completedAt, recurrenceType, familyZone, Instant.now());
    }

    public static boolean isWithinCurrentPeriod(
            Instant completedAt, RecurrenceType recurrenceType, ZoneId familyZone, Instant referenceInstant) {
        ZonedDateTime completed = completedAt.atZone(familyZone);
        ZonedDateTime reference = referenceInstant.atZone(familyZone);

        return switch (recurrenceType) {
            case NONE -> true;
            case DAILY, CUSTOM -> completed.toLocalDate().equals(reference.toLocalDate());
            case WEEKLY -> completed.get(WEEK_FIELDS.weekOfWeekBasedYear()) == reference.get(WEEK_FIELDS.weekOfWeekBasedYear())
                    && completed.get(WEEK_FIELDS.weekBasedYear()) == reference.get(WEEK_FIELDS.weekBasedYear());
            case MONTHLY -> completed.getYear() == reference.getYear() && completed.getMonth() == reference.getMonth();
        };
    }
}
