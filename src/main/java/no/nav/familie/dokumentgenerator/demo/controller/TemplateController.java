package no.nav.familie.dokumentgenerator.demo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.familie.dokumentgenerator.demo.services.TemplateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@CrossOrigin(origins = {"http://localhost:3000"})
@RestController
public class TemplateController {

    private final TemplateService templateManagementService;

    public TemplateController(TemplateService templateManagementService) {
        this.templateManagementService = templateManagementService;
    }

    @GetMapping("/mal/alle")
    public List<String> getAllTemplateNames() {
        return templateManagementService.getTemplateSuggestions();
    }

    @GetMapping(value = "/mal/{templateName}", produces = "text/plain")
    public String getTemplateContentInMarkdown(@PathVariable String templateName) {
        return templateManagementService.getMarkdownTemplate(templateName);
    }

    @PostMapping(value = "/mal/{format}/{templateName}", consumes = "application/json")
    public ResponseEntity setTemplateContent(@PathVariable String format,
                                             @PathVariable String templateName,
                                             @RequestBody String payload) {
        try {
            JsonNode jsonContent = templateManagementService.getJsonFromString(payload);
            /*templateManagementService.saveTemplateFile(
                    templateName,
                    jsonContent.get("markdownContent").textValue()
            );*/

            JsonNode testSet = templateManagementService.getTestSetField(
                    templateName,
                    jsonContent.get("testSetName").textValue()
            );

            return templateManagementService.returnConvertedLetter(
                    templateName,
                    testSet,
                    format
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @PutMapping(value = "/mal/{format}/{templateName}", consumes = "application/json")
    public ResponseEntity updateTemplateContent(@PathVariable String format,
                                                @PathVariable String templateName,
                                                @RequestBody String payload) {
        try {
            JsonNode jsonContent = templateManagementService.getJsonFromString(payload);
            templateManagementService.saveTemplateFile(
                    templateName,
                    jsonContent.get("markdownContent").textValue()
            );

            JsonNode testSet = templateManagementService.getTestSetField(
                    templateName,
                    jsonContent.get("testSetName").textValue()
            );
            
            return templateManagementService.returnConvertedLetter(
                    templateName,
                    testSet,
                    format
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @PostMapping(value = "/brev/{format}/{templateName}", consumes = "application/json")
    public ResponseEntity getTemplateContentInHtml(@PathVariable String format,
                                                   @PathVariable String templateName,
                                                   @RequestBody String payload) {
        try{
            JsonNode jsonContent = templateManagementService.getJsonFromString(payload);
            return templateManagementService.returnConvertedLetter(
                    templateName,
                    jsonContent.get("interleavingFields"),
                    format
            );
        }
        catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }

    @GetMapping(value = "maler/{templateName}/testdata")
    public ResponseEntity<List<String>> getTestData(@PathVariable String templateName) {
        List<String> response = templateManagementService.getTestdataNames(templateName);

        if (response == null) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(templateManagementService.getTestdataNames(templateName), HttpStatus.OK);
    }
}
