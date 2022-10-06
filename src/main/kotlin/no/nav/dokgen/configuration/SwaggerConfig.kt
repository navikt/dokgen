package no.nav.dokgen.configuration

import org.springdoc.core.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.hateoas.client.LinkDiscoverers
import org.springframework.hateoas.mediatype.collectionjson.CollectionJsonLinkDiscoverer
import org.springframework.plugin.core.SimplePluginRegistry


@Configuration
class SwaggerConfig {
    @Bean
    open fun publicApi(): GroupedOpenApi? {
        return GroupedOpenApi.builder()
            .group("springshop-public")
            .pathsToMatch("/public/**")
            .build()
    }

    @Primary
    @Bean
    fun discoverers(): LinkDiscoverers? {
        val plugins = listOf(CollectionJsonLinkDiscoverer())
        return LinkDiscoverers(SimplePluginRegistry.create(plugins))
    }
}