package no.nav.dokgen.services;

import no.nav.dokgen.util.FileStructureUtil;
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

public class TestDataServiceTest {
    private static final String TEST_DATA = "{\"key\": \"value\"}";
    private static final String TEST_DATA_NAME = "navn";
    private static final String TEMPLATE_NAME = "mal";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private TestDataService testdataService;
    private Path contentRoot;
    private Path testDataPath;

    @Before
    public void setUp() throws IOException {
        contentRoot = temporaryFolder.getRoot().toPath();
        testdataService = new TestDataService(contentRoot, new JsonService(contentRoot));
        testDataPath = FileStructureUtil.getTestDataRootPath(contentRoot, TEMPLATE_NAME);
        Files.createDirectories(testDataPath);
    }

    @Test
    public void skalLagreNyttTestsett() throws IOException {
        FileUtils.writeStringToFile(FileStructureUtil.getTemplateSchemaPath(contentRoot, TEMPLATE_NAME).toFile(),
                "{}",
                StandardCharsets.UTF_8
        );

        assertThat(testdataService.saveTestData(TEMPLATE_NAME, TEST_DATA_NAME, TEST_DATA)).isEqualTo(TEST_DATA_NAME);
    }

    @Test
    public void skalHenteTestdatasettNavn() throws IOException {

        Files.createFile(testDataPath.resolve("1.json"));
        Files.createFile(testDataPath.resolve("2.json"));

        assertThat(testdataService.listTestData(TEMPLATE_NAME)).containsExactlyInAnyOrder("1", "2");
    }


}
