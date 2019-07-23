package no.nav.familie.dokumentgenerator.demo.services;

import com.fasterxml.jackson.databind.JsonNode;
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
import no.nav.familie.dokumentgenerator.demo.utils.FileUtils;
import no.nav.familie.dokumentgenerator.demo.utils.GenerateUtils;
import no.nav.familie.dokumentgenerator.demo.utils.JsonUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;


@Service
public class TemplateService {
    private Handlebars handlebars;
    private GenerateUtils generateUtils;
    private JsonUtils jsonUtils;
    private FileUtils fileUtils;


    private Handlebars getHandlebars() {
        return handlebars;
    }

    private void setHandlebars(Handlebars handlebars) {
        this.handlebars = handlebars;
    }

    private void setGenerateUtils(GenerateUtils generateUtils) {
        this.generateUtils = generateUtils;
    }

    private void setJsonUtils(JsonUtils jsonUtils) {
        this.jsonUtils = jsonUtils;
    }

    private void setFileUtils(FileUtils fileUtils) {
        this.fileUtils = fileUtils;
    }

    private Template compileTemplate(String templateName) {
        try {
            return this.getHandlebars().compile(fileUtils.getTemplatePath(templateName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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

    @PostConstruct
    public void loadHandlebarTemplates() {
        TemplateLoader loader = new ClassPathTemplateLoader("/", null);
        setHandlebars(new Handlebars(loader));
        setFileUtils(new FileUtils());
        setGenerateUtils(new GenerateUtils());
        setJsonUtils(new JsonUtils());
    }

    public List<String> getTemplateSuggestions() {
        return fileUtils.getResourceNames("templates");
    }

    public String getMarkdownTemplate(String templateName) {
        String content = null;
        String path = fileUtils.getTemplatePath(templateName);
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

    public ResponseEntity returnLetterResponse(String format, String templateName, String payload, boolean useTestSet){
        try{
            JsonNode jsonContent = jsonUtils.getJsonFromString(payload);

            JsonNode valueFields = jsonUtils.extractInterleavingFields(
                    templateName,
                    jsonContent,
                    useTestSet
            );

            return returnConvertedLetter(templateName, valueFields, format);
        }
        catch (IOException e){
            e.printStackTrace();
        }

        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public ResponseEntity saveAndReturnTemplateResponse(String format, String templateName, String payload, boolean useTestSet) {
        try{
            JsonNode jsonContent = jsonUtils.getJsonFromString(payload);

            fileUtils.saveTemplateFile(
                    templateName,
                    jsonContent.get("markdownContent").textValue()
            );

            JsonNode valueFields = jsonUtils.extractInterleavingFields(
                    templateName,
                    jsonContent,
                    useTestSet
            );

            return returnConvertedLetter(templateName, valueFields, format);
        }
        catch (IOException e){
            e.printStackTrace();
        }

        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity returnConvertedLetter(String templateName, JsonNode interleavingFields, String format) {
        String compiledTemplate = getCompiledTemplate(templateName, interleavingFields);

        if (format.equals("html")) {
            if(compiledTemplate == null){
                return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
            }
            Document document = Jsoup.parse((compiledTemplate));
            Element head = document.head();
            head.append("<meta charset=\"UTF-8\">");
            head.append(("<link rel=\"stylesheet\" href=\"css/main.css\">"));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            return new ResponseEntity<>(generateUtils.convertMarkdownTemplateToHtml((compiledTemplate)), headers, HttpStatus.OK);
        } else if (format.equals("pdf") || format.equals("pdfa")) {
            String htmlConvertedTemplate = generateUtils.convertMarkdownTemplateToHtml(compiledTemplate);
            String styledHtml = generateUtils.appendHtmlMetadata(htmlConvertedTemplate).html();
            byte[] pdfContent = generateUtils.generatePDF(styledHtml, templateName);

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

    public List<String> getTestdataNames(String templateName) {
        String path = String.format("templates/%s/testdata/", templateName);
        return fileUtils.getResourceNames(path);
    }

    public String validateTestData(String name, String json) {
        return jsonUtils.validateTestData(name, json);
    }
}
