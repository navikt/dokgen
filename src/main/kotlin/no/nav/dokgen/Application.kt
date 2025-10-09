package no.nav.dokgen

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication


@SpringBootApplication
class DokgenApplication


fun main(args: Array<String>) {
    SpringApplication.run(DokgenApplication::class.java, *args)
}