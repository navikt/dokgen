package no.nav.dokgen.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
public class HateoasServiceTests {

    @Before
    public void setUp() {

    }

    @Test
    public void skal_returnere_template_links() {
        var links = HateoasService.templateLinks("test");
        assertThat(links.getLinks().size()).isGreaterThan(0);
        for (var link : links.getLinks()) {
            assertThat(link.getRel()).isNotEmpty();
            assertThat(link.getHref()).isNotEmpty();
        }
    }

    @Test
    public void skal_returnere_testdata_links() {
        var links = HateoasService.testDataLinks("test", "test");
        assertThat(links.getLinks().size()).isGreaterThan(0);
        for (var link : links.getLinks()) {
            assertThat(link.getRel()).isNotEmpty();
            assertThat(link.getHref()).isNotEmpty();
        }
    }
}
