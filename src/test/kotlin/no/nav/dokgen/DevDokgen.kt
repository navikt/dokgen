package no.nav.dokgen

import no.nav.dokgen.configuration.ApplicationConfig
import org.springframework.boot.builder.SpringApplicationBuilder

object DevDokgen {
    @JvmStatic
    fun main(args: Array<String>) {
        SpringApplicationBuilder(ApplicationConfig::class.java)
            .profiles("dev")
            .run(*args)
    }
}