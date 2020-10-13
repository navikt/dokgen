package no.nav.dokgen.configuration

import no.nav.dokgen.services.TemplateService
import org.slf4j.LoggerFactory
import org.springframework.context.ConfigurableApplicationContext

class DelayedShutdownHook(private val applicationContext: ConfigurableApplicationContext) : Thread() {
    override fun run() {
        try {
            // https://github.com/kubernetes/kubernetes/issues/64510
            // https://nav-it.slack.com/archives/C5KUST8N6/p1543497847341300
            sleep(5000L)
        } catch (e: InterruptedException) {
            LOG.error("En feil med shutdown oppstod", e)
        }
        applicationContext.close()
        super.run()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(DelayedShutdownHook::class.java)
    }
}