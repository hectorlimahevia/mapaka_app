package cat.mapaka.common;

import org.springframework.http.HttpStatus;

/** Excepció de domini amb un codi estable per al frontend (Família+.pdf secció 61). */
public class DomainException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public DomainException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }
}
