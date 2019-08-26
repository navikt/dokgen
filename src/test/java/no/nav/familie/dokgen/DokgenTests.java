package no.nav.familie.dokgen;

import no.nav.familie.dokgen.Dokgen;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = Dokgen.class)
@ActiveProfiles("dev")
public class DokgenTests {

    @Test
    public void contextLoads() {
    }

}
