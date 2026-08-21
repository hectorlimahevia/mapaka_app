package cat.mapaka.family;

public record FamilySettingsUpdateRequest(
        boolean taskApprovalRequired, boolean notifyPendingApprovalsEnabled, boolean allowSavingsTransfer) {
}
