package no.nav.dokgen.configuration

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.util.UrlPathHelper

@Configuration
class WebConfig(val loggerInterceptor: LoggerInterceptor) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(loggerInterceptor)
    }

}

@Configuration
@ConditionalOnProperty("allow-encoded-slash")
class EnableSlashInURLPath : WebMvcConfigurer {
    override fun configurePathMatch(configurer: PathMatchConfigurer) {
        val urlPathHelper = UrlPathHelper()
        urlPathHelper.isUrlDecode = false
        configurer.setUrlPathHelper(urlPathHelper)
    }
}