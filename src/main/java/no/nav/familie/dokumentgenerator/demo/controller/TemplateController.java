package no.nav.familie.dokumentgenerator.demo.controller;

import no.nav.familie.dokumentgenerator.demo.model.TemplateService;
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

        return templateManagementService.convertMarkdownTemplateToHtml(compiledMarkdownTemplate);
    }

    @PostMapping(value = "maler/{name}", consumes = "text/plain")
    public void setTemplateContent(@RequestBody String content) {
    }
}
