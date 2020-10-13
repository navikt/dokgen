package no.nav.dokgen.resources

data class TemplateResource(var name: String,
                            var content: String? = null,
                            var compiledContent: String? = null,
)