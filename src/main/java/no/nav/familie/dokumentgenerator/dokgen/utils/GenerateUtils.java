package no.nav.familie.dokumentgenerator.dokgen.utils;

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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class GenerateUtils {

    private FileUtils fileUtils = FileUtils.getInstance();

    public void addDocumentParts(Document document){
        String resourceLocation = fileUtils.getContentRoot() + "assets/htmlParts/";
        try{

            String header = new String(Files.readAllBytes(Paths.get(resourceLocation + "headerTemplate.html")));
            String footer = new String(Files.readAllBytes(Paths.get(resourceLocation + "footerTemplate.html")));

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
        head.append("<style>" + fileUtils.getCss(cssName) + "</style>");

        return document;
    }

    public void generatePDF(Document html, ByteArrayOutputStream outputStream) {
        org.w3c.dom.Document doc = new W3CDom().fromJsoup(html);

        PdfRendererBuilder builder = new PdfRendererBuilder();
        try{
            byte[] colorProfile = IOUtils.toByteArray(new FileInputStream(fileUtils.getContentRoot() + "sRGB2014.icc"));

            builder
                    .useFont(
                            new File(fileUtils.getContentRoot() + "assets/fonts/fontpack/SourceSansPro-Regular.ttf"),
                            "Source Sans Pro",
                            400,
                            BaseRendererBuilder.FontStyle.NORMAL,
                            false
                    )
                    .useFont(
                            new File(fileUtils.getContentRoot() + "assets/fonts/fontpack/SourceSansPro-Bold.ttf"),
                            "Source Sans Pro",
                            700,
                            BaseRendererBuilder.FontStyle.OBLIQUE,
                            false
                    )
                    .useFont(
                            new File(fileUtils.getContentRoot() + "assets/fonts/fontpack/SourceSansPro-Italic.ttf"),
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
