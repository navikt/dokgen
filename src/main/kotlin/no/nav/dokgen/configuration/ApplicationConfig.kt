package no.nav.dokgen.configuration

import no.nav.familie.log.filter.LogFilter
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.convert.ApplicationConversionService
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.format.FormatterRegistry
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.util.UrlPathHelper


@SpringBootConfiguration
@ComponentScan("no.nav.dokgen")
class ApplicationConfig {

    @Bean
    fun logFilter(): FilterRegistrationBean<LogFilter> {
        log.info("Registering LogFilter filter")
        val filterRegistration = FilterRegistrationBean<LogFilter>()
        filterRegistration.filter = LogFilter()
        filterRegistration.order = 1
        return filterRegistration
    }

    @Bean
    fun requestTimeFilter(): FilterRegistrationBean<CustomRequestTimeFilter> {
        log.info("Registering RequestTimeFilter")
        val filterRegistration = FilterRegistrationBean<CustomRequestTimeFilter>()
        filterRegistration.filter = CustomRequestTimeFilter()
        filterRegistration.order = 2
        return filterRegistration
    }

    @Bean
    @ConditionalOnProperty("allow-encoded-slash")
    fun enableSlashInURLPath(): WebMvcConfigurer {
        return object : WebMvcConfigurer {
            override fun configurePathMatch(configurer: PathMatchConfigurer) {
                val urlPathHelper = UrlPathHelper()
                urlPathHelper.isUrlDecode = false
                configurer.setUrlPathHelper(urlPathHelper)
            }
        }
    }
    companion object {
        private val log = LoggerFactory.getLogger(ApplicationConfig::class.java)
    }
}