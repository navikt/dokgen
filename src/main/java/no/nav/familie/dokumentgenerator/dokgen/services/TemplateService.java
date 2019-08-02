package no.nav.familie.dokumentgenerator.dokgen.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.JsonNodeValueResolver;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.context.FieldValueResolver;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.context.MethodValueResolver;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import no.nav.familie.dokumentgenerator.dokgen.utils.FileUtils;
import no.nav.familie.dokumentgenerator.dokgen.utils.GenerateUtils;
import no.nav.familie.dokumentgenerator.dokgen.utils.JsonUtils;
import org.everit.json.schema.ValidationException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;


@Service
public class TemplateService {
    private Handlebars handlebars;
    private GenerateUtils generateUtils;
    private JsonUtils jsonUtils;
    private FileUtils fileUtils;

    public TemplateService() {
        this.fileUtils = FileUtils.getInstance();
    }

    public TemplateService(String contentRoot) {
        this.fileUtils = FileUtils.getInstance(contentRoot);
    }

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

    private Template compileTemplate(String templateName) {
        try {
            return this.getHandlebars().compile(templateName + "/" + templateName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getCompiledTemplate(String templateName, JsonNode interleavingFields) throws ValidationException {
        try {
            Template template = compileTemplate(templateName);

            jsonUtils.validateTestData(templateName, interleavingFields.toString());
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

    private HttpHeaders genHtmlHeaders(){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);

        return headers;
    }

    private HttpHeaders genPdfHeaders(String templateName){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        String filename = templateName + ".pdf";
        headers.setContentDispositionFormData("inline", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return headers;
    }

    @PostConstruct
    public void loadHandlebarTemplates() {
        TemplateLoader loader = new FileTemplateLoader(new File(this.fileUtils.getContentRoot() + "templates/").getPath());
        setHandlebars(new Handlebars(loader));
        setGenerateUtils(new GenerateUtils());
        setJsonUtils(new JsonUtils());
    }

    public List<String> getTemplateSuggestions() {
        return fileUtils.getResourceNames(this.fileUtils.getContentRoot() + "templates/");
    }

    public String getMarkdownTemplate(String templateName) {
        String content = null;
        String path = fileUtils.getTemplatePath(templateName);
        try {
            content = new String(Files.readAllBytes(Paths.get(path)));
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Kunne ikke åpne template malen");
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
        String compiledTemplate;

        try {
            compiledTemplate = getCompiledTemplate(templateName, interleavingFields);
        }
        catch (ValidationException e) {
            return new ResponseEntity<>(e.toJSON().toString(), HttpStatus.BAD_REQUEST);
        }

        if(compiledTemplate == null){
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }

        if (format.equals("html")) {
            Document styledHtml = generateUtils.appendHtmlMetadata(compiledTemplate, "html");
            return new ResponseEntity<>(styledHtml.html(), genHtmlHeaders(), HttpStatus.OK);
        } else if (format.equals("pdf") || format.equals("pdfa")) {
            Document styledHtml = generateUtils.appendHtmlMetadata(compiledTemplate, "pdf");
            generateUtils.addDocumentParts(styledHtml);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            generateUtils.generatePDF(styledHtml, outputStream);
            byte[] pdfContent = outputStream.toByteArray();

            if (pdfContent == null) {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }

            return new ResponseEntity<>(pdfContent, genPdfHeaders(templateName), HttpStatus.OK);
        }
        return null;
    }

    public List<String> getTestdataNames(String templateName) {
        String path = String.format(this.fileUtils.getContentRoot() + "templates/%s/testdata/", templateName);
        return fileUtils.getResourceNames(path);
    }


    public String getEmptyTestSet(String templateName) {
        return jsonUtils.getEmptyTestData(templateName);
    }

    public ResponseEntity createTestSet(String templateName, String payload) {

        JSONObject obj = new JSONObject(payload);
        String testSetName = obj.getString("name");
        String testSetContent = obj.getJSONObject("content").toString(4);
        String createdFileName;

        try{
            jsonUtils.validateTestData(templateName, testSetContent);
            createdFileName = fileUtils.createNewTestSet(templateName, testSetName, testSetContent);
        }
        catch (ValidationException e) {
            return new ResponseEntity<>(e.toJSON().toString(), HttpStatus.BAD_REQUEST);
        }
        catch (IOException e){
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(createdFileName, HttpStatus.CREATED);
    }
}
