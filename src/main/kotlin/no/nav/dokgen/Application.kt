package no.nav.dokgen

import no.nav.dokgen.configuration.ApplicationConfig
import no.nav.dokgen.configuration.DelayedShutdownHook
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ConfigurableApplicationContext

@SpringBootApplication
class DokgenApplication

fun main(args: Array<String>) {
    val app = SpringApplication(ApplicationConfig::class.java)
    app.setRegisterShutdownHook(false)
    val applicationContext: ConfigurableApplicationContext = app.run(*args)
    Runtime.getRuntime().addShutdownHook(DelayedShutdownHook(applicationContext))
}
