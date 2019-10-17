package no.nav.dokgen.services;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import com.openhtmltopdf.util.XRLog;
import no.nav.dokgen.util.DocFormat;
import no.nav.dokgen.util.FileStructureUtil;
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
import org.springframework.util.FileCopyUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class DocumentGeneratorService {
    private static final Logger LOG = LoggerFactory.getLogger(DocumentGeneratorService.class);
    private static final Charset UTF_8 = StandardCharsets.UTF_8;
    private final Path contentRoot;

    @Autowired
    public DocumentGeneratorService(@Value("${path.content.root:./content/}") Path contentRoot) {
        this.contentRoot = contentRoot;
        XRLog.setLoggingEnabled(false);
    }

    public void wrapDocument(Document document, DocFormat format) {
        try {
            String header = Files.readString(FileStructureUtil.getFormatHeader(contentRoot, format), UTF_8);
            String footer = Files.readString(FileStructureUtil.getFormatFooter(contentRoot, format), UTF_8);
            Element body = document.body();
            body.prepend(header);
            body.append(footer);
        } catch (IOException e) {
            throw new RuntimeException("Kunne ikke legge til header/footer å dokumentet", e);
        }
    }

    public Document appendHtmlMetadata(String markdown, DocFormat format) {
        String convertedTemplate = convertMarkdownTemplateToHtml(markdown);

        Document document = Jsoup.parse(("<div id=\"content\">" + convertedTemplate + "</div>"));
        Element head = document.head();

        head.append("<meta charset=\"UTF-8\">");
        head.append("<style>" + hentCss(format) + "</style>");

        return document;
    }

    public static byte[] getColorProfile() throws IOException {
        ClassPathResource cpr = new ClassPathResource("sRGB2014.icc");
        byte[] colorProfile = FileCopyUtils.copyToByteArray(cpr.getInputStream());
        return colorProfile;
    }

    public void genererPDF(Document html, ByteArrayOutputStream outputStream) {
        org.w3c.dom.Document doc = new W3CDom().fromJsoup(html);
        PdfRendererBuilder builder = new PdfRendererBuilder();
        try {
            builder
                    .useFont(
                            FileStructureUtil.getFont(contentRoot, "SourceSansPro-Regular.ttf").toFile(),
                            "Source Sans Pro",
                            400,
                            BaseRendererBuilder.FontStyle.NORMAL,
                            true
                    )
                    .useFont(
                            FileStructureUtil.getFont(contentRoot, "SourceSansPro-Bold.ttf").toFile(),
                            "Source Sans Pro",
                            700,
                            BaseRendererBuilder.FontStyle.OBLIQUE,
                            true
                    )
                    .useFont(
                            FileStructureUtil.getFont(contentRoot, "SourceSansPro-lt.ttf").toFile(),
                            "Source Sans Pro",
                            400,
                            BaseRendererBuilder.FontStyle.ITALIC,
                            true
                    )
                    .useColorProfile(getColorProfile())
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

    private String hentCss(DocFormat format) {
        try {
            Path cssPath = FileStructureUtil.getCss(contentRoot, format);
            return Files.readString(cssPath, UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Kan ikke hente " + format + ".css", e);
        }
    }

    private Parser getMarkdownToHtmlParser() {
        return Parser.builder().build();
    }

    private HtmlRenderer getHtmlRenderer() {
        return HtmlRenderer.builder().build();
    }
}
