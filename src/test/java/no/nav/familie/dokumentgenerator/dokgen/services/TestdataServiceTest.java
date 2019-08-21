package no.nav.familie.dokumentgenerator.dokgen.services;

import no.nav.familie.dokumentgenerator.dokgen.util.MalUtil;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class TestdataServiceTest {
    private static final String TESTSETT_JSON = "{\"key\": \"value\"}";

    private static final String TESTSETT_NAVN = "navn";
    private static final String GYLDIG_PAYLOAD = "{\"name\": \"" + TESTSETT_NAVN + "\"" +
            ", \"content\": " + TESTSETT_JSON +
            "}";
    private static final String TEMPLATES_MAL = "templates/mal/";
    private static final String MAL = "mal";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private TestdataService testdataService;
    private Path contentRoot;
    private Path testDataPath;

    @Before
    public void setUp() throws IOException {
        contentRoot = temporaryFolder.getRoot().toPath();
        testdataService = new TestdataService(contentRoot, new JsonService(contentRoot));
        testDataPath = contentRoot.resolve(TEMPLATES_MAL + "testdata/");
        Files.createDirectories(testDataPath);
    }

    @Test
    public void skalHenteTomtTestsettForMal() throws IOException {
        Path tomtTestsett = contentRoot.resolve(TEMPLATES_MAL + "/TomtTestsett.json");
        Files.createFile(tomtTestsett);
        Files.write(tomtTestsett, TESTSETT_JSON.getBytes());

        assertThat(testdataService.hentTomtTestsett(MAL)).isEqualTo(TESTSETT_JSON);
    }

    @Test
    public void skalLagreNyttTestsett() throws IOException {

        FileUtils.writeStringToFile(MalUtil.hentJsonSchemaForMal(contentRoot, MAL).toFile(),
                "{}",
                StandardCharsets.UTF_8
        );

        assertThat(testdataService.lagTestsett(MAL, GYLDIG_PAYLOAD)).isEqualTo(TESTSETT_NAVN);
    }

    @Test
    public void skalHenteTestdatasettNavn() throws IOException {

        Files.createFile(testDataPath.resolve("1.json"));
        Files.createFile(testDataPath.resolve("2.json"));

        assertThat(testdataService.hentTestdatasettForMal(MAL)).containsExactlyInAnyOrder("1", "2");
    }


}
