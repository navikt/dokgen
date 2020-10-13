package no.nav.dokgen.exceptions

class DokgenValidationException(val validationErrors: Map<String, String>, message: String?, cause: Throwable?) :
    RuntimeException(message, cause)