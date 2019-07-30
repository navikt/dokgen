package no.nav.familie.dokumentgenerator.dokgen;

import no.nav.familie.dokumentgenerator.dokgen.services.TemplateService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DokgenTests {

    @Test
    public void contextLoads() {
    }

    @Test
    public void testGetTemplateSuggestions() {
        List<String> expectedResult = new ArrayList<>() {
            {
                add("Avslag");
                add("Innvilget");
                add("Mangelbrev");
                add("Varsel");
            }
        };

        TemplateService templateService = new TemplateService();
        templateService.loadHandlebarTemplates();
        List<String> actualResult = templateService.getTemplateSuggestions();
        Assert.assertEquals(expectedResult.toArray().length, actualResult.toArray().length);
    }
}
