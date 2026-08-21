package cat.mapaka.auth;

/** Token temporal (10 min) que només permet cridar POST /api/auth/recover/reset-pin. */
public record RecoverResponse(String recoveryToken) {
}
