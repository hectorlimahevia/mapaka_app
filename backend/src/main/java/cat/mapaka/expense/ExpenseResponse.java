package cat.mapaka.expense;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExpenseResponse(
        UUID id, UUID childId, String childName, BigDecimal amount, String reason,
        ExpenseStatus status, Instant createdAt) {

    public static ExpenseResponse from(Expense e) {
        return new ExpenseResponse(
                e.getId(), e.getChild().getId(), e.getChild().getDisplayName(),
                e.getAmount(), e.getReason(), e.getStatus(), e.getCreatedAt());
    }
}
