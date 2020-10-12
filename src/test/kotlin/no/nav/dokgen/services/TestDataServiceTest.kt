package no.nav.dokgen.services

import no.nav.dokgen.util.FileStructureUtil.getTemplateSchemaPath
import no.nav.dokgen.util.FileStructureUtil.getTestDataRootPath
import org.apache.commons.io.FileUtils
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class TestDataServiceTest {
    @get:Rule
    var temporaryFolder = TemporaryFolder()

    private lateinit var testdataService: TestDataService
    private lateinit var contentRoot: Path
    private lateinit var testDataPath: Path
    @Before
    @Throws(IOException::class)
    fun setUp() {
        contentRoot = temporaryFolder.root.toPath()
        testdataService = TestDataService(contentRoot, JsonService(contentRoot))
        testDataPath = getTestDataRootPath(contentRoot, TEMPLATE_NAME)
        Files.createDirectories(testDataPath)
    }

    @Test
    @Throws(IOException::class)
    fun skalLagreNyttTestsett() {
        FileUtils.writeStringToFile(
            getTemplateSchemaPath(contentRoot, TEMPLATE_NAME).toFile(),
            "{}",
            StandardCharsets.UTF_8
        )
        Assertions.assertThat(testdataService.saveTestData(TEMPLATE_NAME, TEST_DATA_NAME, TEST_DATA)).isEqualTo(
            TEST_DATA_NAME
        )
    }

    @Test
    @Throws(IOException::class)
    fun skalHenteTestdatasettNavn() {
        Files.createFile(testDataPath.resolve("1.json"))
        Files.createFile(testDataPath.resolve("2.json"))
        Assertions.assertThat(testdataService.listTestData(TEMPLATE_NAME)).containsExactlyInAnyOrder("1", "2")
    }

    companion object {
        private const val TEST_DATA = "{\"key\": \"value\"}"
        private const val TEST_DATA_NAME = "navn"
        private const val TEMPLATE_NAME = "mal"
    }
}