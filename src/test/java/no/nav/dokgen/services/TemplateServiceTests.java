package no.nav.dokgen.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.catchThrowable;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.github.jknack.handlebars.Template;

import no.nav.dokgen.resources.TemplateResource;
import no.nav.dokgen.util.FileStructureUtil;

public class TemplateServiceTests {
    private static final String MALNAVN = "lagretMal";
    private static final String MARKDOWN = "# Hei, {{name}}";
    private static final String MARKDOWN_CONTENT = "\"" + MARKDOWN + "\"";
    private static final String MERGE_FIELDS = "{\"name\": \"Peter\"}";
    private static final String GYLDIG_PAYLOAD = lagTestPayload(MARKDOWN_CONTENT, MERGE_FIELDS);
    private static final String FOOTER_CONTENT = "this would be the footer.";

    private static String lagTestPayload(String markdownContent, String interleavingFields) {
        return "{\"markdownContent\": " + markdownContent +
                ", \"interleavingFields\": " + interleavingFields +
                ", \"useTestSet\": false}";
    }

    public static Path getTestContentPath() {
        try {
            return Paths.get(
                    DocumentGeneratorServiceTests.class.getProtectionDomain().getCodeSource().getLocation().toURI()
            ).resolve(
                    Paths.get("test-content")
            ).toAbsolutePath();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private static final String TOM_JSON = "{}";

    private TemplateService malService;
    private Path contentRoot;
    private Path templateRoot;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        contentRoot = temporaryFolder.getRoot().toPath();
        templateRoot = FileStructureUtil.getTemplateRootPath(contentRoot);
        Files.createDirectories(templateRoot);
        FileWriter fileWriter = new FileWriter(templateRoot.resolve("footer.hbs").toString());
        fileWriter.write("\n" + FOOTER_CONTENT);
        fileWriter.close();
        malService = new TemplateService(contentRoot, new DocumentGeneratorService(getTestContentPath()), new JsonService(contentRoot));
    }

    @Test
    public void skalHentAlleMalerReturnererTomtSett() {
        assertThat(malService.listTemplates()).isEmpty();
    }

    @Test
    public void skalHentAlleMaler() throws IOException {
        Files.createDirectory(templateRoot.resolve("MAL1"));
        assertThat(malService.listTemplates()).containsExactly("MAL1");
    }

    @Test
    public void skalReturnereExceptionVedHentingAvMal() {

        // expect
        Throwable thrown = catchThrowable(() -> malService.getTemplate("IkkeEksisterendeMal"));

        // when
        assertThat(thrown).isInstanceOf(RuntimeException.class).hasMessage("Kan ikke finne mal med navn IkkeEksisterendeMal");
    }

    @Test
    public void skalReturnereMal() throws IOException {
        Path malPath = templateRoot.resolve("MAL");
        Files.createDirectories(malPath);
        Path templatePath = FileStructureUtil.getTemplatePath(contentRoot, "MAL");
        Files.write(templatePath, "maldata".getBytes());


        // expect
        TemplateResource template = malService.getTemplate("MAL");

        // when
        assertThat(template.getContent()).isEqualTo("maldata");
    }

    @Test
    public void skalReturnereIllegalArgumentExceptionVedLagreMalUtenPayloadUtenMarkdown() {

        // expect
        Throwable thrown = catchThrowable(() -> malService.saveTemplate("tomPayload", "{}"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("Kan ikke hente markdown for payload={}");
    }

    @Test
    public void skalLagreMal() throws IOException {

        // expect
        malService.saveTemplate(MALNAVN, GYLDIG_PAYLOAD);

        String malData = Files.readString(FileStructureUtil.getTemplatePath(contentRoot, MALNAVN));
        assertThat(malData).isEqualTo(MARKDOWN);
    }


    @Test
    public void skal_compile_inline_with_a_footer() throws IOException {
        Template tmp = malService.compileInLineTemplate("this would be a header\n{{> footer }}");
        JsonNode node = JsonNodeFactory.instance.arrayNode();
        String compiledStr = tmp.apply(node);
        assertThat(compiledStr).endsWith(FOOTER_CONTENT);
    }

    @Test
    public void skal_overskrive_alt_ved_lagring_av_nytt_slankere_innhold() throws IOException {
        skalLagreMal();

        malService.saveTemplate(MALNAVN, lagTestPayload("\"" + MARKDOWN.substring(3) + "\"", MERGE_FIELDS));

        String malData = Files.readString(FileStructureUtil.getTemplatePath(contentRoot, MALNAVN));
        assertThat(malData).isEqualTo(MARKDOWN.substring(3));
    }

    @Test
    public void skalHenteHtml() throws IOException {
        String malNavn = "html";

        FileUtils.writeStringToFile(FileStructureUtil.getTemplateSchemaPath(contentRoot, malNavn).toFile(),
                TOM_JSON,
                StandardCharsets.UTF_8
        );

        // expect
        malService.saveTemplate(malNavn, GYLDIG_PAYLOAD);
        String html = malService.createHtml(malNavn, "{\"name\":\"Peter\"}");
        assertThat(html).contains("<h1>Hei, Peter</h1>");
    }

    @Test
    public void skalHentePdf() throws IOException {
        String malNavn = "pdf";

        FileUtils.writeStringToFile(FileStructureUtil.getTemplateSchemaPath(contentRoot, malNavn).toFile(),
                TOM_JSON,
                StandardCharsets.UTF_8
        );

        // expect
        malService.saveTemplate(malNavn, GYLDIG_PAYLOAD);
        byte[] pdf = malService.createPdf(malNavn, GYLDIG_PAYLOAD);
        assertThat(pdf[1]).isEqualTo((byte) 0x50);//P
        assertThat(pdf[2]).isEqualTo((byte) 0x44);//D
        assertThat(pdf[3]).isEqualTo((byte) 0x46);//F
    }

    @Test
    public void skalHentePdfFraVariation() throws IOException {
        String malNavn = "pdf";
        String templateVariation = "template_NN";

        FileUtils.writeStringToFile(FileStructureUtil.getTemplateSchemaPath(contentRoot, malNavn).toFile(),
                TOM_JSON,
                StandardCharsets.UTF_8
        );

        // expect
        malService.saveTemplate(malNavn, GYLDIG_PAYLOAD, templateVariation);
        byte[] pdf = malService.createPdf(malNavn, GYLDIG_PAYLOAD, templateVariation);
        assertThat(pdf[1]).isEqualTo((byte) 0x50);//P
        assertThat(pdf[2]).isEqualTo((byte) 0x44);//D
        assertThat(pdf[3]).isEqualTo((byte) 0x46);//F
    }
}
