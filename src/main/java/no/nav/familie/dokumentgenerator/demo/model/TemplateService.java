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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
//        this.handlebars = handlebars.registerHelper("md", new MarkdownHelper());
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

    private Document appendHtmlMetadata(String html) {
        Document document = Jsoup.parse(html);
        Element head = document.head();
        head.append("<meta charset=\"UTF-8\">");
//        head.append(("<link rel=\"stylesheet\" href=\"css/main.css\">"));
        head.append("<style>\n" +
                "    * {\n" +
                "        font-family: \"Source Sans Pro\" !important;\n" +
                "    }\n" +
                "    \n" +
                "    .line_top {\n" +
                "        border-top: 1px black solid;\n" +
                "    }\n" +
                "    \n" +
                "    .line_bottom {\n" +
                "        border-bottom: 1px black solid;\n" +
                "    }\n" +
                "    \n" +
                "    .line_left {\n" +
                "        border-left: 1px black solid;\n" +
                "    }\n" +
                "    \n" +
                "    .line_right {\n" +
                "        border-right: 1px black solid;\n" +
                "    }\n" +
                "    \n" +
                "    .block {\n" +
                "        width: 100%;\n" +
                "        margin-top: 0;\n" +
                "    }\n" +
                "    \n" +
                "    .inline {\n" +
                "        display: inline-block;\n" +
                "    }\n" +
                "    \n" +
                "    .main_title {\n" +
                "        margin-bottom: 0;\n" +
                "        margin-top: 2mm;\n" +
                "        margin-left: 2mm;\n" +
                "        vertical-align: bottom;\n" +
                "    }\n" +
                "    \n" +
                "    h1 {\n" +
                "        font-size: 22px;\n" +
                "        margin-bottom: 0;\n" +
                "        margin-top: 0;\n" +
                "    }\n" +
                "    \n" +
                "    h6 {\n" +
                "        margin-bottom: 0;\n" +
                "        margin-top: 0;\n" +
                "    }\n" +
                "    \n" +
                "    p {\n" +
                "        font-size: 12px;\n" +
                "        margin-left: 1mm;\n" +
                "        margin-top: 0;\n" +
                "        margin-right: 1mm;\n" +
                "    }\n" +
                "    \n" +
                "    h5 {\n" +
                "        padding-top: 1mm;\n" +
                "        margin-left: 1mm;\n" +
                "        font-size: 12px;\n" +
                "        margin-top: 0;\n" +
                "    }\n" +
                "    \n" +
                "    h4 {\n" +
                "        padding-top: 2mm;\n" +
                "        margin-left: 1mm;\n" +
                "        font-size: 13px;\n" +
                "        margin-top: 0;\n" +
                "    }\n" +
                "    \n" +
                "    h3 {\n" +
                "        padding-top: 3mm;\n" +
                "        margin-left: 1mm;\n" +
                "        margin-top: 0;\n" +
                "        margin-bottom: 0;\n" +
                "        font-size: 14px;\n" +
                "        font-style: italic;\n" +
                "    }\n" +
                "    \n" +
                "    tr {\n" +
                "        font-size: 10px;\n" +
                "    }\n" +
                "    \n" +
                "    .bold {\n" +
                "        font-weight: bold;\n" +
                "    }\n" +
                "    \n" +
                "    .checkbox {\n" +
                "        width: 3mm;\n" +
                "        height: 3mm;\n" +
                "        border: black 1px solid;\n" +
                "        text-align: center;\n" +
                "        vertical-align: middle;\n" +
                "        font-size: 10px;\n" +
                "    }\n" +
                "    \n" +
                "    .new_page {\n" +
                "        page-break-before: always;\n" +
                "    }\n" +
                "    \n" +
                "    .checkbox_title_right {\n" +
                "        margin-left: 5mm;\n" +
                "        font-size: 12px;\n" +
                "        vertical-align: middle;\n" +
                "    }\n" +
                "    \n" +
                "    .titled_checkbox_block {\n" +
                "        margin-left: 3mm;\n" +
                "        margin-bottom: 5mm;\n" +
                "    }\n" +
                "    \n" +
                "    .titled_checkbox_inline {\n" +
                "        display: inline;\n" +
                "        margin-left: 3mm;\n" +
                "    }\n" +
                "    \n" +
                "    .signature_date_title {\n" +
                "        font-size: 10px;\n" +
                "    }\n" +
                "    \n" +
                "    .centered_text_title {\n" +
                "        text-align: center;\n" +
                "        font-size: 12px;\n" +
                "    }\n" +
                "</style>\n");
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
        BufferedWriter writer = new BufferedWriter(new FileWriter(ClassLoader.getSystemResource("templates/" + tempName).getPath(), false));
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
                .uri(URI.create(this.getPdfGenURl() + applicationName))
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
