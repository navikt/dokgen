package no.nav.dokgen.services

import com.openhtmltopdf.pdfboxout.visualtester.PdfVisualTester
import com.openhtmltopdf.pdfboxout.visualtester.PdfVisualTester.PdfCompareResult
import no.nav.dokgen.configuration.ContentProperties
import no.nav.dokgen.util.DocFormat
import org.apache.commons.io.IOUtils
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors
import javax.imageio.ImageIO

class DocumentGeneratorServiceTests {

    private val documentGeneratorService = DocumentGeneratorService(contentPropertiesTestFixture)

    @Throws(IOException::class)
    private fun runTest(resource: String, actualPdfBytes: ByteArray): Boolean {
        Files.createDirectories(Paths.get(TEST_OUTPUT_PATH))

        // Load expected PDF document from resources, change class below.
        var expectedPdfBytes: ByteArray
        DocumentGeneratorServiceTests::class.java.getResourceAsStream("$EXPECTED_RES_PATH$resource.pdf")
            .use { expectedIs -> expectedPdfBytes = IOUtils.toByteArray(expectedIs) }

        // Get a list of results.
        val problems = PdfVisualTester.comparePdfDocuments(expectedPdfBytes, actualPdfBytes, resource, false)
        if (problems.isNotEmpty()) {
            System.err.println("Found problems with test case ($resource):")
            System.err.println(problems.stream().map { p: PdfCompareResult -> p.logMessage }
                .collect(Collectors.joining("\n    ", "[\n    ", "\n]")))
            System.err.println("For test case ($resource) writing failure artifacts to '$TEST_OUTPUT_PATH'")
            val outPdf = File(TEST_OUTPUT_PATH, "$resource---actual.pdf")
            Files.write(outPdf.toPath(), actualPdfBytes)
        }
        for (result in problems) {
            if (result.testImages != null) {
                var output = File(TEST_OUTPUT_PATH, resource + "---" + result.pageNumber + "---diff.png")
                ImageIO.write(result.testImages.createDiff(), "png", output)
                output = File(TEST_OUTPUT_PATH, resource + "---" + result.pageNumber + "---actual.png")
                ImageIO.write(result.testImages.actual, "png", output)
                output = File(TEST_OUTPUT_PATH, resource + "---" + result.pageNumber + "---expected.png")
                ImageIO.write(result.testImages.expected, "png", output)
            }
        }
        return problems.isEmpty()
    }

    @Throws(IOException::class)
    private fun getDocumentFromHtmlFixture(fixtureName: String): String {
        return ClassPathResource("/test-fixtures/expected-html/$fixtureName.html").getContentAsString(StandardCharsets.UTF_8)
    }

    @Test
    @Throws(IOException::class)
    fun testPdfGeneration() {
        val html = getDocumentFromHtmlFixture("minimal1")
        val doc = documentGeneratorService.appendHtmlMetadata(html, DocFormat.PDF)
        val outputStream = ByteArrayOutputStream()
        documentGeneratorService.genererPDF(doc, outputStream)
        val actualBytes = outputStream.toByteArray()
        assertTrue(isPdf(actualBytes))
        assertTrue(runTest("minimal1", actualBytes))
    }

    @Test
    @Throws(IOException::class)
    fun testPdfGenerationWithSvg() {
        val html = getDocumentFromHtmlFixture("svg1")
        val doc = documentGeneratorService.appendHtmlMetadata(html, DocFormat.PDF)
        documentGeneratorService.wrapDocument(doc, DocFormat.PDF) { header: String? -> header }
        val outputStream = ByteArrayOutputStream()
        documentGeneratorService.genererPDF(doc, outputStream)
        val actualBytes = outputStream.toByteArray()
        assertTrue(isPdf(actualBytes))
        assertTrue(runTest("svg1", actualBytes))
    }

    @Test
    @Throws(IOException::class)
    fun testPdfGenerationWithList() {
        val html = getDocumentFromHtmlFixture("list1")
        val doc = documentGeneratorService.appendHtmlMetadata(html, DocFormat.PDF)
        documentGeneratorService.wrapDocument(doc, DocFormat.PDF) { header -> header }
        val outputStream = ByteArrayOutputStream()
        documentGeneratorService.genererPDF(doc, outputStream)
        val actualBytes = outputStream.toByteArray()
        assertTrue(isPdf(actualBytes))
        // save the actualBytes-file or show it to me
        Files.write(Paths.get("target/list1_som_avviker.pdf"), actualBytes)
        assertTrue(runTest("list1", actualBytes))
    }

    companion object {
        private const val TEST_OUTPUT_PATH = "target/regression-tests/"
        private const val EXPECTED_RES_PATH = "/test-fixtures/expected-pdf/"
        val testContentPath: Path = ClassPathResource("/test-content").file.toPath().toAbsolutePath()
        val contentPropertiesTestFixture: ContentProperties = ContentProperties().apply {
            root = testContentPath
        }

        /**
         * Test if the data in the given byte array represents a PDF file.
         */
        fun isPdf(data: ByteArray): Boolean {
            if (data[0] == 0x25.toByte() && // %
                data[1] == 0x50.toByte() && // P
                data[2] == 0x44.toByte() && // D
                data[3] == 0x46.toByte() && // F
                data[4] == 0x2D.toByte()    // -
            ) {

                // version 1.3 file terminator
                if (data[data.size - 7] == 0x25.toByte() && // %
                    data[data.size - 6] == 0x25.toByte() && // %
                    data[data.size - 5] == 0x45.toByte() && // E
                    data[data.size - 4] == 0x4F.toByte() && // O
                    data[data.size - 3] == 0x46.toByte() && // F
                    data[data.size - 2] == 0x20.toByte() && // SPACE
                    data[data.size - 1] == 0x0A.toByte()    // EOL
                ) {
                    return true
                }

                // version 1.3 file terminator
                if (data[data.size - 6] == 0x25.toByte() && // %
                    data[data.size - 5] == 0x25.toByte() && // %
                    data[data.size - 4] == 0x45.toByte() && // E
                    data[data.size - 3] == 0x4F.toByte() && // O
                    data[data.size - 2] == 0x46.toByte() && // F
                    data[data.size - 1] == 0x0A.toByte()    // EOL
                ) {
                    return true
                }
            }
            return false
        }
    }
}