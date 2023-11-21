package no.nav.dokgen.configuration

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.hateoas.client.LinkDiscoverers
import org.springframework.hateoas.mediatype.collectionjson.CollectionJsonLinkDiscoverer
import org.springframework.plugin.core.SimplePluginRegistry


@Configuration
class SwaggerConfig {
    @Bean
    fun swaggerOpenAPI(): OpenAPI? {
        return OpenAPI()
            .info(
                Info().title("Dokgen swagger")
                    .description("Genererer pdf eller html dokumenter basert på markdown og handlebars")
                    .version("v0.0.1")
                    .license(License().name("MIT").url("http://nav.no"))
            )
    }

    @Primary
    @Bean
    fun discoverers(): LinkDiscoverers? {
        val plugins = listOf(CollectionJsonLinkDiscoverer())
        return LinkDiscoverers(SimplePluginRegistry.of(plugins))
    }
}