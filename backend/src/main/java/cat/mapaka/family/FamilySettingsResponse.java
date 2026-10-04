package cat.mapaka.family;

public record FamilySettingsResponse(
        boolean taskApprovalRequired, boolean allowSavingsTransfer, String familyCode) {

    public static FamilySettingsResponse from(Family family) {
        return new FamilySettingsResponse(
                family.isTaskApprovalRequired(), family.isAllowSavingsTransfer(), family.getFamilyCode());
    }
}
