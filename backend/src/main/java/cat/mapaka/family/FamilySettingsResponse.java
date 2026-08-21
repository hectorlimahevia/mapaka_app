package cat.mapaka.family;

public record FamilySettingsResponse(
        boolean taskApprovalRequired, boolean notifyPendingApprovalsEnabled, boolean allowSavingsTransfer) {

    public static FamilySettingsResponse from(Family family) {
        return new FamilySettingsResponse(
                family.isTaskApprovalRequired(), family.isNotifyPendingApprovalsEnabled(), family.isAllowSavingsTransfer());
    }
}
