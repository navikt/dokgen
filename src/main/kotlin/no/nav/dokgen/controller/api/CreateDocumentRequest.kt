package no.nav.dokgen.controller.api

import no.nav.dokgen.util.DocFormat

data class CreateDocumentRequest(
        var docFormat: DocFormat? = null,
        var templateContent: String? = null,
        var isPrecompiled: Boolean = false,
        var mergeFields: String? = null,
        var isIncludeHeader: Boolean = false,
        var headerFields: String? = null,
)