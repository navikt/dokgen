package no.nav.familie.dokgen.feil;

import java.util.Map;

public class DokgenValideringException extends RuntimeException {
    private final Map<String,String> valideringsFeil;

    public DokgenValideringException(Map<String, String> valideringsFeil, String message, Throwable cause) {
        super(message, cause);
        this.valideringsFeil = valideringsFeil;
    }

    public Map<String, String> getValideringsFeil() {
        return valideringsFeil;
    }
}
