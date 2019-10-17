package no.nav.dokgen.exceptions;

import java.util.Map;

public class DokgenValidationException extends RuntimeException {
    private final Map<String,String> validationErrors;

    public DokgenValidationException(Map<String, String> validationErrors, String message, Throwable cause) {
        super(message, cause);
        this.validationErrors = validationErrors;
    }

    public Map<String, String> getValidationErrors() {
        return validationErrors;
    }
}
