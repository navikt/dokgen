package no.nav.dokgen.util

enum class DocFormat {
    PDF, HTML, EMAIL;

    override fun toString(): String {
        return name.toLowerCase()
    }
}