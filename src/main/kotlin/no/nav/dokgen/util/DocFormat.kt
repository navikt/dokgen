package no.nav.dokgen.util

enum class DocFormat {
    PDF, HTML, EMAIL, pdfinntektsmelding;

    override fun toString(): String {
        return name.lowercase()
    }
}