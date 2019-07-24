package no.nav.familie.dokumentgenerator.demo.controller;

import no.nav.familie.dokumentgenerator.demo.services.TemplateService;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        return templateManagementService.returnLetterResponse(
                format,
                templateName,
                payload,
                true
        );
    }

    @PutMapping(value = "/mal/{format}/{templateName}", consumes = "application/json")
    public ResponseEntity updateTemplateContent(@PathVariable String format,
                                                @PathVariable String templateName,
                                                @RequestBody String payload) {
        return templateManagementService.saveAndReturnTemplateResponse(
                format,
                templateName,
                payload,
                true
        );
    }

    @PostMapping(value = "/brev/{format}/{templateName}", consumes = "application/json")
    public ResponseEntity getTemplateContentInHtml(@PathVariable String format,
                                                   @PathVariable String templateName,
                                                   @RequestBody String payload) {
        return templateManagementService.returnLetterResponse(
                format,
                templateName,
                payload,
                false
        );
    }

    @GetMapping(value = "maler/{templateName}/testdata")
    public ResponseEntity<List<String>> getTestData(@PathVariable String templateName) {
        List<String> response = templateManagementService.getTestdataNames(templateName);

        if (response == null) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(templateManagementService.getTestdataNames(templateName), HttpStatus.OK);
    }

    @GetMapping(value = "maler/{templateName}/tomtTestSett", produces = "application/json")
    public ResponseEntity<String> getEmptyTestSet(@PathVariable String templateName) {
        return new ResponseEntity<>(templateManagementService.getEmptyTestSet(templateName), HttpStatus.OK);
    }

    @PostMapping(value="maler/{templateName}/nyttTestSett", consumes = "application/json")
    public void setNewTestSet(@PathVariable String templateName, @RequestBody String payload) {
        JSONObject obj = new JSONObject(payload);
        String testSetName = obj.getString("name");
        String testSetContent = obj.getString("content");
        templateManagementService.createNewTestSet(templateName, testSetName, testSetContent);
    }
}
