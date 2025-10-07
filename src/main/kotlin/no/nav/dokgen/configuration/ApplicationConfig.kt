package no.nav.dokgen.configuration

import no.nav.familie.log.NavSystemtype
import no.nav.familie.log.filter.LogFilter
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.EventListener


@SpringBootConfiguration
@ComponentScan("no.nav.dokgen")
class ApplicationConfig {

    @Bean
    fun logFilter(): FilterRegistrationBean<LogFilter> {
        log.info("Registering LogFilter filter")
        val filterRegistration = FilterRegistrationBean<LogFilter>()
        filterRegistration.filter = LogFilter(NavSystemtype.NAV_SAKSBEHANDLINGSSYSTEM)
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

    @EventListener
    fun onApplicationEvent(event: ContextClosedEvent) {
        // https://docs.nais.io/workloads/explanations/good-practices/?h=sigterm#handles-termination-gracefully
        log.info("Mottok SIGTERM, venter 5 sekunder på at app tas ut av lastbalanserer")
        Thread.sleep(5000L)
    }

    companion object {
        private val log = LoggerFactory.getLogger(ApplicationConfig::class.java)
    }
}