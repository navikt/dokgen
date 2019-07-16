package no.nav.familie.dokumentgenerator.demo.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.*;
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
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
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


    private Handlebars getHandlebars() {
        return handlebars;
    }

    private void setHandlebars(Handlebars handlebars) {
//        this.handlebars = handlebars.registerHelper("md", new MarkdownHelper());
        this.handlebars = handlebars;
    }

    @PostConstruct
    public void loadHandlebarTemplates() {
        TemplateLoader loader = new ClassPathTemplateLoader("/templates", ".hbs");
        setHandlebars(new Handlebars(loader));
    }

    private Template getTemplate(String templateName) throws IOException {
        return this.getHandlebars().compile(templateName);
    }

    private URL getJsonPath(String templateName) {
        return ClassLoader.getSystemResource("json/" + templateName);
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
    private String getCssFile(String fileName) throws IOException {
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

    private Document appendHtmlMetadata(String html) {
        Document document = Jsoup.parse(html);
        Element head = document.head();
        head.append("<meta charset=\"UTF-8\">");
//        head.append(("<link rel=\"stylesheet\" href=\"css/main.css\">"));

        try{
            head.append(getCssFile("main.css"));
        }
        catch (Exception e){
            System.out.println("No css provided");
        }
        return document;
    }

    public List<String> getTemplateSuggestions() throws IOException {
        List<String> templateNames = new ArrayList<>();
        File folder = new ClassPathResource("templates").getFile();
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null) {
            return null;
        }

        for (File file : listOfFiles) {
            templateNames.add(FilenameUtils.getBaseName(file.getName()));
        }
        return templateNames;
    }


    public String getCompiledTemplate(String name) throws IOException {
        Template template = getTemplate(name);
        URL path = getJsonPath(name + ".json");
        JsonNode jsonNode = readJsonFile(path);
        return template.apply(insertTemplateContent(jsonNode));
    }

    public String convertMarkdownTemplateToHtml(String content) {
        Parser parser = getMarkdownParser();
        Node document = parser.parse(content);
        HtmlRenderer renderer = getHtmlRenderer();
        return renderer.render(document);
    }

    public void writeToFile(String name, String content) throws IOException {
        String tempName = name + ".hbs";
        BufferedWriter writer = new BufferedWriter(new FileWriter(ClassLoader.getSystemResource("templates/" + tempName).getPath(), false));
        writer.append(content);
        writer.close();
    }
}
