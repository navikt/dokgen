package no.nav.familie.dokumentgenerator.demo.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.JsonNodeValueResolver;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.context.FieldValueResolver;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.context.MethodValueResolver;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import org.apache.commons.io.FilenameUtils;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TemplateService {
    private Handlebars handlebars;
    private String pdfGenURl = "http://localhost:8090/api/v1/genpdf/html/";

    private Handlebars getHandlebars() {
        return handlebars;
    }

    private void setHandlebars(Handlebars handlebars) {
        this.handlebars = handlebars;
    }

    private Template compileTemplate(String templateName) {
        try {
            return this.getHandlebars().compile(getTemplatePath(templateName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private String getTemplatePath(String templateName) {
        return String.format("templates/%1$s/%1$s.hbs", templateName);
    }

    private String getPdfGenURl() {
        return pdfGenURl;
    }

    private Node parseDocument(String content) {
        return getMarkdownToHtmlParser().parse(content);
    }

    private String renderToHTML(Node document) {
        return getHtmlRenderer().render(document);
    }

    private List<String> getResourceNames(String path) {
        List<String> resourceNames = new ArrayList<>();
        File folder;
        File[] listOfFiles;
        try {
            folder = new ClassPathResource(path).getFile();
            listOfFiles = folder.listFiles();

            if (listOfFiles == null) {
                return null;
            }

            for (File file : listOfFiles) {
                resourceNames.add(FilenameUtils.getBaseName(file.getName()));
            }

            return resourceNames;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Parser getMarkdownToHtmlParser() {
        return Parser.builder().build();
    }

    private HtmlRenderer getHtmlRenderer() {
        return HtmlRenderer.builder().build();
    }

    private URL getCssPath(String cssName) {
        return ClassLoader.getSystemResource("static/css/" + cssName);
    }

    private JsonNode readJsonFile(URL path) {
        if (path != null) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                return mapper.readTree(new File(path.toURI()));
            } catch (IOException | URISyntaxException e) {
                System.out.println("Kan ikke finne JSON fil!");
                e.printStackTrace();
            }
        }
        return null;
    }
    private String getCssFile(String fileName) {
        URI filePath = null;
        try{
            filePath = getCssPath(fileName).toURI();
        }
        catch (URISyntaxException e){
            e.printStackTrace();
        }

        StringBuilder sb = new StringBuilder();
        List<String> stringList = new ArrayList<>();
        if(filePath != null){
            try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
                stringList = stream.collect(Collectors.toList());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for(String line : stringList){
            sb.append(line);
        }

        return sb.toString();
    }

    private Document appendHtmlMetadata(String html) {

        Document document = Jsoup.parse(html);
        Element head = document.head();
        String css = getCssFile("main.css");

        head.append("<meta charset=\"UTF-8\">");
        head.append("\n<style>\n" + css + "\n</style>");

        return document;
    }

    private String getCompiledTemplate(String templateName, JsonNode interleavingFields) {
        try {
            Template template = compileTemplate(templateName);
            if(template != null){
                return template.apply(insertTestData(interleavingFields));
            }
            return null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Context insertTestData(JsonNode model) {
        return Context
                .newBuilder(model)
                .resolver(JsonNodeValueResolver.INSTANCE,
                        JavaBeanValueResolver.INSTANCE,
                        FieldValueResolver.INSTANCE,
                        MapValueResolver.INSTANCE,
                        MethodValueResolver.INSTANCE
                ).build();
    }

    private void writeToFile(String name, String content) throws IOException {
        String tempName = name + ".hbs";
        BufferedWriter writer = new BufferedWriter(
                new FileWriter(
                        ClassLoader.getSystemResource
                                ("templates/" + tempName).getPath(), false));
        writer.append(content);
        writer.close();
    }

    private String convertMarkdownTemplateToHtml(String content) {
        Node document = parseDocument(content);
        String html = renderToHTML(document);
        Document htmlDocument = appendHtmlMetadata(html);
        return htmlDocument.html();
    }

    private byte[] generatePDF(String html, String applicationName) {
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
        return  null;
    }


    @PostConstruct
    public void loadHandlebarTemplates() {
        TemplateLoader loader = new ClassPathTemplateLoader("/", null);
        setHandlebars(new Handlebars(loader));
    }

    public List<String> getTemplateSuggestions() {
        return getResourceNames("templates");
    }

    public String getMarkdownTemplate(String templateName) {
        String content = null;
        String path = getTemplatePath(templateName);
        try {
            content = new String(Files.readAllBytes(Paths.get(ClassLoader.getSystemResource(path).toURI())));
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Kunne ikke åpne template malen");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            System.out.println("Kunne ikke finne handlebars malen");
        }
        return content;
    }

    public JsonNode getTestSetField(String templateName, String testSet){
        URL path = ClassLoader.getSystemResource("templates/" + templateName + "/testdata/" + testSet + ".json");
        return readJsonFile(path);
    }

    public JsonNode getJsonFromString(String json) throws IOException{
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(json);
    }

    public ResponseEntity returnConvertedLetter(String templateName, JsonNode interleavingFields, String format) {
        String compiledTemplate = getCompiledTemplate(templateName, interleavingFields);

        if (format.equals("html")) {
            String html = convertMarkdownTemplateToHtml(compiledTemplate);

            Document document = Jsoup.parse(html);
            Element head = document.head();
            head.append("<meta charset=\"UTF-8\">");
            head.append(("<link rel=\"stylesheet\" href=\"css/main.css\">"));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            return new ResponseEntity<>(convertMarkdownTemplateToHtml(html), headers, HttpStatus.OK);
        } else if (format.equals("pdf") || format.equals("pdfa")) {
            String htmlConvertedTemplate = convertMarkdownTemplateToHtml(compiledTemplate);
            byte[] pdfContent = generatePDF(htmlConvertedTemplate, templateName);

            if (pdfContent == null) {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = templateName + ".pdf";
            headers.setContentDispositionFormData("inline", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
        }
        return null;
    }

    public void saveTemplateFile(String templateName, String markdownContent) {
        Document.OutputSettings settings = new Document.OutputSettings();
        settings.prettyPrint(false);
        String strippedHtmlSyntax = Jsoup.clean(
                markdownContent,
                "",
                Whitelist.none(),
                settings
        );

        try {
            writeToFile(templateName, strippedHtmlSyntax);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getTestdataNames(String templateName) {
        String path = String.format("templates/%s/testdata/", templateName);
        return getResourceNames(path);
    }
}
