package no.nav.dokgen.exceptions;

public class DokgenNotFoundException extends RuntimeException {
    public DokgenNotFoundException(String message) {
        super(message);
    }
}
