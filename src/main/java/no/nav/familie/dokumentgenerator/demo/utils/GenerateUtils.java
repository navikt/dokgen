package no.nav.familie.dokumentgenerator.demo.utils;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class GenerateUtils {
    private String pdfGenURl = "http://localhost:8090/api/v1/genpdf/html/";

    private String getPdfGenURl() {
        return pdfGenURl;
    }

    public void addDocumentParts(Document document){
        String resourceLocation = "./content/assets/htmlParts/";
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
        head.append("<link rel=\"stylesheet\" href=\"http://localhost:8080/css/" + cssName + ".css\">");

        return document;
    }

    private String convertMarkdownTemplateToHtml(String content) {
        Node document = parseDocument(content);
        return renderToHTML(document);
    }

    public byte[] generatePDF(String html, String applicationName) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getPdfGenURl() + applicationName))
                .header("Content-Type", "text/html;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(html))
                .build();

        try {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200) {
                return response.body();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
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
