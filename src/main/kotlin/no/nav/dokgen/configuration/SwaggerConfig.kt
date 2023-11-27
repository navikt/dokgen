package no.nav.dokgen.configuration

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.hateoas.server.LinkRelationProvider
import org.springframework.hateoas.server.core.DefaultLinkRelationProvider


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

    @Bean
    fun linkRelationProvider(): LinkRelationProvider {
        return DefaultLinkRelationProvider()
    }
}