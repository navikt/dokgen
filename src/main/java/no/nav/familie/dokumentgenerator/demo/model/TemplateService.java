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
import org.springframework.core.io.ClassPathResource;
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
import java.util.ArrayList;
import java.util.List;

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

    private Template getTemplate(String templateName) throws IOException {
        return this.getHandlebars().compile(templateName);
    }

    private String getPdfGenURl() {
        return pdfGenURl;
    }

    private URL getJsonPath(String templateName) {
        return ClassLoader.getSystemResource("json/" + templateName);
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

    private Context insertTemplateContent(JsonNode model) {
        return Context
                .newBuilder(model)
                .resolver(JsonNodeValueResolver.INSTANCE,
                        JavaBeanValueResolver.INSTANCE,
                        FieldValueResolver.INSTANCE,
                        MapValueResolver.INSTANCE,
                        MethodValueResolver.INSTANCE
                ).build();
    }

    private Parser getMarkdownParser() {
        return Parser.builder().build();
    }

    private HtmlRenderer getHtmlRenderer() {
        return HtmlRenderer.builder().build();
    }

    private Document appendHtmlMetadata(String html)  {
        Document document = Jsoup.parse(html);
        Element head = document.head();
        head.append("<meta charset=\"UTF-8\">");
//        head.append(("<link rel=\"stylesheet\" href=\"css/main.css\">"));
        return document;
    }

    @PostConstruct
    public void loadHandlebarTemplates() {
        TemplateLoader loader = new ClassPathTemplateLoader("/templates", ".hbs");
        setHandlebars(new Handlebars(loader));
    }

    public List<String> getTemplateSuggestions() {
        List<String> templateNames = new ArrayList<>();
        File folder;
        File[] listOfFiles;
        try {
            folder = new ClassPathResource("templates").getFile();
            listOfFiles = folder.listFiles();

            if (listOfFiles == null) {
                return null;
            }

            for (File file : listOfFiles) {
                templateNames.add(FilenameUtils.getBaseName(file.getName()));
            }

            return templateNames;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


    public String getCompiledTemplate(String name)  {
        try {
            Template template = getTemplate(name);
            URL path = getJsonPath(name + ".json");
            JsonNode jsonNode = readJsonFile(path);
            return template.apply(insertTemplateContent(jsonNode));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String convertMarkdownTemplateToHtml(String content) {
        Parser parser = getMarkdownParser();
        Node document = parser.parse(content);
        HtmlRenderer renderer = getHtmlRenderer();
        return renderer.render(document);
    }

    public void writeToFile(String name, String content) throws IOException {
        String tempName = name + ".hbs";
        BufferedWriter writer = new BufferedWriter(
                new FileWriter(
                        ClassLoader.getSystemResource
                                ("templates/" + tempName).getPath(), false));
        writer.append(content);
        writer.close();
    }

    public byte[] generatePDF(String applicationName) {
        String template = getCompiledTemplate(applicationName);

        if (template == null) {
            return null;
        }

        String html = convertMarkdownTemplateToHtml(template);
        Document document = appendHtmlMetadata(html);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getPdfGenURl() + applicationName))
                .header("Content-Type", "text/html;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(document.html()))
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
}
