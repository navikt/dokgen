package no.nav.familie.dokumentgenerator.demo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.familie.dokumentgenerator.demo.model.TemplateService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;
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

    @GetMapping(value = "/mal", produces = "text/plain")
    public String getTemplateContentInMarkdown(@RequestParam String templateName) {
        return templateManagementService.getUncompiledTemplate(templateName);
    }

    @PostMapping(value = "/mal", consumes = "application/json")
    public ResponseEntity setTemplateContent(@RequestBody String payload) {

        try {
            JsonNode jsonContent = templateManagementService.getJsonFromString(payload);
            templateManagementService.saveTemplateFile(
                    jsonContent.get("templateName").textValue(),
                    jsonContent.get("markdownContent").textValue()
            );

            return templateManagementService.returnConvertedLetter(
                    jsonContent.get("templateName").asText(),
                    jsonContent.get("interleavingFields"),
                    jsonContent.get("format").asText()
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @PutMapping(value = "/mal", consumes = "application/json")
    public ResponseEntity updateTemplateContent(@RequestBody String payload) {

        try {
            JsonNode jsonContent = templateManagementService.getJsonFromString(payload);
            templateManagementService.saveTemplateFile(
                    jsonContent.get("templateName").textValue(),
                    jsonContent.get("markdownContent").textValue()
            );

            return templateManagementService.returnConvertedLetter(
                    jsonContent.get("templateName").asText(),
                    jsonContent.get("interleavingFields"),
                    jsonContent.get("format").asText()
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @PostMapping(value = "/brev")
    public ResponseEntity getTemplateContentInHtml(@RequestBody String payload) {
        try{
            JsonNode jsonContent = templateManagementService.getJsonFromString(payload);
            return templateManagementService.returnConvertedLetter(
                    jsonContent.get("templateName").asText(),
                    jsonContent.get("interleavingFields"),
                    jsonContent.get("format").asText()
            );
        }
        catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }
}
