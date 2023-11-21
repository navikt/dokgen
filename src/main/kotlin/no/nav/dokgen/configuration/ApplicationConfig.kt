package no.nav.dokgen.configuration

import no.nav.familie.log.filter.LogFilter
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan

@SpringBootConfiguration
@ComponentScan("no.nav.dokgen")
class ApplicationConfig {

    @Bean
    fun logFilter(): FilterRegistrationBean<LogFilter> {
        log.info("Registering LogFilter filter")
        val filterRegistration: FilterRegistrationBean<LogFilter> = FilterRegistrationBean()
        filterRegistration.filter = LogFilter()
        filterRegistration.order = 1
        return filterRegistration
    }

    companion object {
        private val log = LoggerFactory.getLogger(ApplicationConfig::class.java)
    }
}