package no.nav.dokgen.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.hateoas.client.LinkDiscoverers
import org.springframework.hateoas.mediatype.collectionjson.CollectionJsonLinkDiscoverer
import org.springframework.plugin.core.SimplePluginRegistry
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2


@Configuration
@EnableSwagger2
class SwaggerConfig {
    @Bean
    fun api(): Docket {
        return Docket(DocumentationType.SWAGGER_2)
            .select()
            .apis(RequestHandlerSelectors.any())
            .paths(PathSelectors.any())
            .build()
    }

    @Primary
    @Bean
    fun discoverers(): LinkDiscoverers? {
        val plugins = listOf(CollectionJsonLinkDiscoverer())
        return LinkDiscoverers(SimplePluginRegistry.create(plugins))
    }
}