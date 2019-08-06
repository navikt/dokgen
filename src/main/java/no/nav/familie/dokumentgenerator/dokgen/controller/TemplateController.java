package no.nav.familie.dokumentgenerator.dokgen.controller;


import org.json.JSONObject;

import no.nav.familie.dokumentgenerator.dokgen.services.TemplateService;
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
                payload
        );
    }

    @PutMapping(value = "/mal/{format}/{templateName}", consumes = "application/json")
    public ResponseEntity updateTemplateContent(@PathVariable String format,
                                                @PathVariable String templateName,
                                                @RequestBody String payload) {
        return templateManagementService.saveAndReturnTemplateResponse(
                format,
                templateName,
                payload
        );
    }

    @GetMapping(value = "mal/{templateName}/testdata")
    public ResponseEntity<List<String>> getTestData(@PathVariable String templateName) {
        List<String> response = templateManagementService.getTestdataNames(templateName);

        if (response == null) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(templateManagementService.getTestdataNames(templateName), HttpStatus.OK);
    }

    @GetMapping(value = "mal/{templateName}/tomtTestSett", produces = "application/json")
    public ResponseEntity<String> getEmptyTestSet(@PathVariable String templateName) {
        return new ResponseEntity<>(templateManagementService.getEmptyTestSet(templateName), HttpStatus.OK);
    }


    @PostMapping(value="mal/{templateName}/nyttTestSett", consumes = "application/json", produces = "application/json")
    public ResponseEntity createNewTestSet(@PathVariable String templateName, @RequestBody String payload) {
        return templateManagementService.createTestSet(templateName, payload);
    }


    @PostMapping(value = "/brev/{format}/{templateName}", consumes = "application/json")
    public ResponseEntity getGeneratedContent(@PathVariable String format,
                                                   @PathVariable String templateName,
                                                   @RequestBody String payload) {
        return templateManagementService.returnLetterResponse(
                format,
                templateName,
                payload
        );
    }

    @PostMapping(value = "/brev/{templateName}/download", consumes = "application/json")
    public ResponseEntity getGeneratedContentDownload(@PathVariable String templateName,
                                                      @RequestBody String payload) {
        return templateManagementService.returnLetterResponseAndDownload(
                templateName,
                payload
        );
    }
}
