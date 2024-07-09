package no.nav.dokgen.util

enum class DocFormat {
    PDF, HTML, EMAIL, PDFINNTEKTSMELDING;

    override fun toString(): String {
        return name.lowercase()
    }
}