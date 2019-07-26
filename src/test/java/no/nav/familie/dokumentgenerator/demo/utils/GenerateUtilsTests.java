package no.nav.familie.dokumentgenerator.demo.utils;

import static org.junit.Assert.assertTrue;
import com.openhtmltopdf.pdfboxout.visualtester.PdfVisualTester;
import com.openhtmltopdf.pdfboxout.visualtester.PdfVisualTester.PdfCompareResult;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

public class GenerateUtilsTests {
    private static final String TEST_OUTPUT_PATH = "target/regression-tests/";
    private static final String EXPECTED_RES_PATH = "/test-fixtures/expected-pdf/";
    private GenerateUtils generateUtils = new GenerateUtils();

    private boolean runTest(String resource, byte[] actualPdfBytes) throws IOException {
        Files.createDirectories(Paths.get(TEST_OUTPUT_PATH));

        // Load expected PDF document from resources, change class below.
        byte[] expectedPdfBytes;

        try (InputStream expectedIs = GenerateUtilsTests.class.getResourceAsStream(EXPECTED_RES_PATH + resource + ".pdf")) {
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

    private Document getDocumentFromHtmlFixture(String fixtureName) throws IOException {
        String genHtml = IOUtils.toString(
                this.getClass().getResourceAsStream("/test-fixtures/expected-html/" + fixtureName + ".html"),
                StandardCharsets.UTF_8
        );
        return Jsoup.parse(genHtml);
    }

    @Test
    public void testPdfGeneration() throws IOException {
        Document doc = getDocumentFromHtmlFixture("minimal1");
        doc.head().append("<meta charset=\"UTF-8\">");
        doc.head().append("<link rel=\"stylesheet\" href=\"http://localhost:8080/css/pdf.css\">");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        generateUtils.generatePDF(doc, outputStream);
        byte[] expectedBytes = outputStream.toByteArray();

        assertTrue(runTest("minimal1", expectedBytes));
    }
}
