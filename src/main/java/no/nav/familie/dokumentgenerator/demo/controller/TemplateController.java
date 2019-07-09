package no.nav.familie.dokumentgenerator.demo.controller;

import no.nav.familie.dokumentgenerator.demo.model.TemplateService;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@CrossOrigin(origins = {"http://localhost:3000"})
@RestController
public class TemplateController {

    private final TemplateService templateManagementService;

    public TemplateController(TemplateService templateManagementService) {
        this.templateManagementService = templateManagementService;
    }

    @GetMapping("/maler")
    public List<String> getAllTemplateNames() {
        try {
            return templateManagementService.getTemplateSuggestions();
        } catch (IOException e) {
            System.out.println("Kunne ikke finne noen maler!");
            e.printStackTrace();
        }
        return null;
    }

    @GetMapping(value = "maler/markdown/{name}", produces = "text/plain")
    public String getTemplateContentInMarkdown(@PathVariable String name) {
        try {
            return templateManagementService.getCompiledTemplate(name);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @GetMapping(value = "maler/html/{name}", produces = "text/plain")
    public String getTemplateContentInHtml(@PathVariable String name) {
        String compiledMarkdownTemplate = null;
        try {
            compiledMarkdownTemplate = templateManagementService.getCompiledTemplate(name);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("templateManagementService.convertMarkdownTemplateToHtml(compiledMarkdownTemplate) = " + templateManagementService.convertMarkdownTemplateToHtml(compiledMarkdownTemplate));
        return templateManagementService.convertMarkdownTemplateToHtml(compiledMarkdownTemplate);
    }

    @PostMapping(value = "maler/{name}", consumes = "text/plain")
    public ResponseEntity<String> setTemplateContent(@PathVariable String name, @RequestBody String content) {
//        System.out.println("Saving content");
//        System.out.println("content = " + content);

        try {
            templateManagementService.writeToFile(name, content);
            return new ResponseEntity<>(templateManagementService.getCompiledTemplate(name), HttpStatus.OK);
        } catch (IOException e) {
            System.out.println("Klarte ikke å skrive til fil");
            e.printStackTrace();
        }
        return null;
    }

    @PostMapping(value = "json/{name}", consumes = "application/json")
    public void validerInnflettningsfelt(@PathVariable String name, @RequestBody String json) {
        
//        System.out.println("name = " + name);
        String file = "json/" + name + "/" + name + ".schema.json";

        try (InputStream inputStream = ClassLoader.getSystemResourceAsStream(file)) {
            if (inputStream != null) {
                JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
                Schema schema = SchemaLoader.load(rawSchema);
                try {
                    schema.validate(new JSONObject(json)); // throws a ValidationException if this object is invalid
                    System.out.println("Gyldig JSON(!)");
                } catch (ValidationException e) {
                    System.out.println("e.getMessage() = " + e.getMessage());
                    e.getCausingExceptions()
                            .stream()
                            .map(ValidationException::getMessage)
                            .forEach(System.out::println);
                }
            } else {
                System.out.println("Kan ikke åpne fiiiiiiiiiiiiiiiiiiiiiiiiiiil :)");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
