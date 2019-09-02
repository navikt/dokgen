package no.nav.familie.dokgen.services;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import com.openhtmltopdf.util.XRLog;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class DokumentGeneratorService {
    private static final Logger LOG = LoggerFactory.getLogger(DokumentGeneratorService.class);
    private static final Charset UTF_8 = StandardCharsets.UTF_8;

    @Autowired
    public DokumentGeneratorService(@Value("${path.content.root:./content/}") Path contentRoot) {
        XRLog.setLoggingEnabled(false);
    }

    public void addDocumentParts(Document document) {
        try {
            String header = Files.readString(new ClassPathResource("htmlParts/headerTemplate.html").getFile().toPath(), UTF_8);
            String footer = Files.readString(new ClassPathResource("htmlParts/footerTemplate.html").getFile().toPath(), UTF_8);

            Element body = document.body();
            body.prepend(header);
            body.append(footer);
        } catch (IOException e) {
            throw new RuntimeException("Kunne ikke legge til header/footer å dokumentet", e);
        }
    }

    public Document appendHtmlMetadata(String html, String cssName) {
        String convertedTemplate = convertMarkdownTemplateToHtml(html);

        Document document = Jsoup.parse(("<div id=\"content\">" + convertedTemplate + "</div>"));
        Element head = document.head();

        head.append("<meta charset=\"UTF-8\">");
        head.append("<style>" + hentCss(cssName) + "</style>");

        return document;
    }

    public void genererPDF(Document html, ByteArrayOutputStream outputStream) {
        org.w3c.dom.Document doc = new W3CDom().fromJsoup(html);

        PdfRendererBuilder builder = new PdfRendererBuilder();
        try {
            byte[] colorProfile = Files.readAllBytes(new ClassPathResource("sRGB2014.icc").getFile().toPath());

            builder
                    .useFont(
                            new ClassPathResource("fonts/fontpack/SourceSansPro-Regular.ttf").getFile(),
                            "Source Sans Pro",
                            400,
                            BaseRendererBuilder.FontStyle.NORMAL,
                            false
                    )
                    .useFont(
                            new ClassPathResource("fonts/fontpack/SourceSansPro-Bold.ttf").getFile(),
                            "Source Sans Pro",
                            700,
                            BaseRendererBuilder.FontStyle.OBLIQUE,
                            false
                    )
                    .useFont(
                            new ClassPathResource("fonts/fontpack/SourceSansPro-Italic.ttf").getFile(),
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
        } catch (IOException e) {
            throw new RuntimeException("Feil ved generering av pdf", e);
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

    private String hentCss(String cssName) {
        try {
            return Files.readString(new ClassPathResource("css/" + cssName + ".css").getFile().toPath(), UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Kan ikke hente " + cssName + ".css", e);
        }
    }

    private Parser getMarkdownToHtmlParser() {
        return Parser.builder().build();
    }

    private HtmlRenderer getHtmlRenderer() {
        return HtmlRenderer.builder().build();
    }
}
