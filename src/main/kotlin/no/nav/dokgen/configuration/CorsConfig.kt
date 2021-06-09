package no.nav.dokgen.configuration

import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter
import javax.servlet.Filter

@Configuration
class CorsConfig {
    @Bean
    fun corsFilter(): FilterRegistrationBean<*> {
        val source = UrlBasedCorsConfigurationSource()
        val bean = FilterRegistrationBean<Filter>()
        val config = CorsConfiguration()
        config.addAllowedOriginPattern("*")
        config.addAllowedMethod("*")
        config.allowCredentials = true
        config.maxAge = 3600L
        config.addAllowedHeader("Content-type")
        source.registerCorsConfiguration("/**", config)
        val corsFilter = CorsFilter(source)
        bean.setFilter(corsFilter)
        bean.order = Ordered.HIGHEST_PRECEDENCE
        return bean
    }
}
