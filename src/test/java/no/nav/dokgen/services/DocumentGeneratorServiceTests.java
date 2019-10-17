package no.nav.dokgen.services;

import static org.junit.Assert.assertTrue;

import com.openhtmltopdf.pdfboxout.visualtester.PdfVisualTester;
import com.openhtmltopdf.pdfboxout.visualtester.PdfVisualTester.PdfCompareResult;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;

import no.nav.dokgen.util.DocFormat;
import org.apache.commons.io.IOUtils;
import org.jsoup.nodes.Document;
import org.junit.Test;

public class DocumentGeneratorServiceTests {
    private static final String TEST_OUTPUT_PATH = "target/regression-tests/";
    private static final String EXPECTED_RES_PATH = "/test-fixtures/expected-pdf/";
    private DocumentGeneratorService documentGeneratorService = new DocumentGeneratorService(getTestContentPath());

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

    /**
     * Test if the data in the given byte array represents a PDF file.
     */
    public static boolean isPdf(byte[] data) {
        if (data != null && data.length > 4 &&
                data[0] == 0x25 && // %
                data[1] == 0x50 && // P
                data[2] == 0x44 && // D
                data[3] == 0x46 && // F
                data[4] == 0x2D) { // -

            // version 1.3 file terminator
            if (data[5] == 0x31 && data[6] == 0x2E && data[7] == 0x33 &&
                    data[data.length - 7] == 0x25 && // %
                    data[data.length - 6] == 0x25 && // %
                    data[data.length - 5] == 0x45 && // E
                    data[data.length - 4] == 0x4F && // O
                    data[data.length - 3] == 0x46 && // F
                    data[data.length - 2] == 0x20 && // SPACE
                    data[data.length - 1] == 0x0A) { // EOL
                return true;
            }

            // version 1.3 file terminator
            if (data[5] == 0x31 && data[6] == 0x2E && data[7] == 0x34 &&
                    data[data.length - 6] == 0x25 && // %
                    data[data.length - 5] == 0x25 && // %
                    data[data.length - 4] == 0x45 && // E
                    data[data.length - 3] == 0x4F && // O
                    data[data.length - 2] == 0x46 && // F
                    data[data.length - 1] == 0x0A) { // EOL
                return true;
            }
        }
        return false;
    }

    private boolean runTest(String resource, byte[] actualPdfBytes) throws IOException {
        Files.createDirectories(Paths.get(TEST_OUTPUT_PATH));

        // Load expected PDF document from resources, change class below.
        byte[] expectedPdfBytes;

        try (InputStream expectedIs = DocumentGeneratorServiceTests.class.getResourceAsStream(EXPECTED_RES_PATH + resource + ".pdf")) {
            expectedPdfBytes = IOUtils.toByteArray(expectedIs);
        }

        // Get a list of results.
        List<PdfCompareResult> problems = PdfVisualTester.comparePdfDocuments(expectedPdfBytes, actualPdfBytes, resource, false);

        if (!problems.isEmpty()) {
            System.err.println("Found problems with test case (" + resource + "):");
            System.err.println(problems.stream().map(p -> p.logMessage).collect(Collectors.joining("\n    ", "[\n    ", "\n]")));

            System.err.println("For test case (" + resource + ") writing failure artifacts to '" + TEST_OUTPUT_PATH + "'");
            File outPdf = new File(TEST_OUTPUT_PATH, resource + "---actual.pdf");
            Files.write(outPdf.toPath(), actualPdfBytes);
        }

        for (PdfVisualTester.PdfCompareResult result : problems) {
            if (result.testImages != null) {
                File output = new File(TEST_OUTPUT_PATH, resource + "---" + result.pageNumber + "---diff.png");
                ImageIO.write(result.testImages.createDiff(), "png", output);

                output = new File(TEST_OUTPUT_PATH, resource + "---" + result.pageNumber + "---actual.png");
                ImageIO.write(result.testImages.getActual(), "png", output);

                output = new File(TEST_OUTPUT_PATH, resource + "---" + result.pageNumber + "---expected.png");
                ImageIO.write(result.testImages.getExpected(), "png", output);
            }
        }

        return problems.isEmpty();
    }

    private String getDocumentFromHtmlFixture(String fixtureName) throws IOException {
        return IOUtils.toString(
                this.getClass().getResourceAsStream("/test-fixtures/expected-html/" + fixtureName + ".html"),
                StandardCharsets.UTF_8
        );
    }

    @Test
    public void testPdfGeneration() throws IOException {
        String html = getDocumentFromHtmlFixture("minimal1");
        Document doc = documentGeneratorService.appendHtmlMetadata(html, DocFormat.PDF);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        documentGeneratorService.genererPDF(doc, outputStream);
        byte[] actualBytes = outputStream.toByteArray();
        assertTrue(isPdf(actualBytes));
        assertTrue(runTest("minimal1", actualBytes));
    }

    @Test
    public void testPdfGenerationWithSvg() throws IOException {
        String html = getDocumentFromHtmlFixture("svg1");
        Document doc = documentGeneratorService.appendHtmlMetadata(html, DocFormat.PDF);
        documentGeneratorService.wrapDocument(doc, DocFormat.PDF);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        documentGeneratorService.genererPDF(doc, outputStream);
        byte[] actualBytes = outputStream.toByteArray();
        assertTrue(isPdf(actualBytes));
        assertTrue(runTest("svg1", actualBytes));
    }

    @Test
    public void testPdfGenerationWithList() throws IOException {
        String html = getDocumentFromHtmlFixture("list1");
        Document doc = documentGeneratorService.appendHtmlMetadata(html, DocFormat.PDF);
        documentGeneratorService.wrapDocument(doc, DocFormat.PDF);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        documentGeneratorService.genererPDF(doc, outputStream);
        byte[] actualBytes = outputStream.toByteArray();
        assertTrue(isPdf(actualBytes));
        assertTrue(runTest("list1", actualBytes));
    }
}
