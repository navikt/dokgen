package no.nav.familie.dokumentgenerator.demo.utils;

import com.openhtmltopdf.extend.FSSupplier;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;

import org.apache.commons.io.IOUtils;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.*;

@Service
public class GenerateUtils {

    public void addDocumentParts(Document document){
        String resourceLocation = "src/main/resources/assets/htmlParts/";
        try{
            FileInputStream headerStream = new FileInputStream(resourceLocation + "headerTemplate.html");
            String header = IOUtils.toString(headerStream, "UTF-8");
            FileInputStream footerStream = new FileInputStream(resourceLocation + "footerTemplate.html");
            String footer = IOUtils.toString(footerStream, "UTF-8");

            Element body = document.body();
            body.prepend(header);
            body.append(footer);
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    public Document appendHtmlMetadata(String html, String cssName) {
        String convertedTemplate = convertMarkdownTemplateToHtml(html);

        Document document = Jsoup.parse(("<div id=\"content\">" + convertedTemplate + "</div>"));
        Element head = document.head();

        head.append("<meta charset=\"UTF-8\">");
        head.append("<link rel=\"stylesheet\" href=\"http://localhost:8080/css/" + cssName + ".css\">");

        return document;
    }

    public void generatePDF(Document html, ByteArrayOutputStream outputStream) {
        org.w3c.dom.Document doc = new W3CDom().fromJsoup(html);

        PdfRendererBuilder builder = new PdfRendererBuilder();
        try{
            byte[] colorProfile = IOUtils.toByteArray(GenerateUtils.class.getResourceAsStream("/sRGB2014.icc"));

            builder
                    .useFont(
                            new File("src/main/resources/assets/fonts/fontpack/SourceSansPro-Regular.ttf"),
                            "Source Sans Pro",
                            400,
                            BaseRendererBuilder.FontStyle.NORMAL,
                            false
                    )
                    .useFont(
                            new File("src/main/resources/assets/fonts/fontpack/SourceSansPro-Bold.ttf"),
                            "Source Sans Pro",
                            700,
                            BaseRendererBuilder.FontStyle.OBLIQUE,
                            false
                    )
                    .useFont(
                            new File("src/main/resources/assets/fonts/fontpack/SourceSansPro-Italic.ttf"),
                            "Source Sans Pro",
                            400,
                            BaseRendererBuilder.FontStyle.ITALIC,
                            false
                    )
                    .useColorProfile(colorProfile)
                    .useSVGDrawer(new BatikSVGDrawer())
                    .usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_2_U)
                    .withW3cDocument(doc, "")
                    .toStream(outputStream)
                    .buildPdfRenderer()
                    .createPDF();
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    private String convertMarkdownTemplateToHtml(String content) {
        Node document = parseDocument(content);
        return renderToHTML(document);
    }

    private Node parseDocument(String content) {
        return getMarkdownToHtmlParser().parse(content);
    }

    private String renderToHTML(Node document) {
        return getHtmlRenderer().render(document);
    }

    private Parser getMarkdownToHtmlParser() {
        return Parser.builder().build();
    }

    private HtmlRenderer getHtmlRenderer() {
        return HtmlRenderer.builder().build();
    }
}
