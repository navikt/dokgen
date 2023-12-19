package no.nav.dokgen.configuration

import org.apache.tomcat.util.buf.EncodedSolidusHandling
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.util.UrlPathHelper


@Configuration
@ConditionalOnProperty("allow-encoded-slash")
class SlashEnabledWebMvcConfig : WebMvcConfigurer {

    override fun configurePathMatch(configurer: PathMatchConfigurer) {
        log.info("Creating WebMvcConfigurerer that enables slash in url path")
        val urlPathHelper = UrlPathHelper()
        urlPathHelper.isUrlDecode = false
        configurer.setUrlPathHelper(urlPathHelper)
    }

    @Bean
    fun tomcatCustomizer(): WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
        log.info("Configuring Tomcat to allow encoded slashes.")
        return WebServerFactoryCustomizer { factory ->
            factory.addConnectorCustomizers(TomcatConnectorCustomizer { connector ->
                connector.encodedSolidusHandling = EncodedSolidusHandling.DECODE.value
            })
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(SlashEnabledWebMvcConfig::class.java)
    }

}