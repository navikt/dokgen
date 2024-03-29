package no.nav.dokgen.services

import no.nav.dokgen.services.HateoasService.templateLinks
import no.nav.dokgen.services.HateoasService.testDataLinks
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class HateoasServiceTests {

    @Test
    fun skal_returnere_template_links() {
        val links = templateLinks("test")
        Assertions.assertThat(links.links).isNotEmpty
        for (link in links.links) {
            Assertions.assertThat(link.rel).isNotNull
            Assertions.assertThat(link.href).isNotEmpty
        }
    }

    @Test
    fun skal_returnere_testdata_links() {
        val links = testDataLinks("test", "test")
        Assertions.assertThat(links.links).isNotEmpty
        for (link in links.links) {
            Assertions.assertThat(link.rel).isNotNull
            Assertions.assertThat(link.href).isNotEmpty
        }
    }
}