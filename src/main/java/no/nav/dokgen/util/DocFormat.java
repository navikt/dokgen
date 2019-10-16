package no.nav.dokgen.util;

public enum DocFormat {
    PDF, HTML, EMAIL;

    public String toString() {
        return name().toLowerCase();
    }
}
