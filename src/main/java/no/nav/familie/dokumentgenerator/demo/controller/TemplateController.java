package no.nav.familie.dokumentgenerator.demo.controller;

import no.nav.familie.dokumentgenerator.demo.model.TemplateService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
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

    @GetMapping(value = "maler/html/{name}", produces = "text/html")
    public String getTemplateContentInHtml(@PathVariable String name) {
        try {
            String compiledMarkdownTemplate = templateManagementService.getCompiledTemplate(name);
            String markdownToHtml = templateManagementService.convertMarkdownTemplateToHtml(compiledMarkdownTemplate);

            Document document = Jsoup.parse(markdownToHtml);
            Element head = document.head();
            head.append("<meta charset=\"UTF-8\">");
//            head.append(("<link rel=\"stylesheet\" href=\"http://example.com/your.css\">"));

            return templateManagementService.convertMarkdownTemplateToHtml(document.html());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @PostMapping(value = "maler/{name}", consumes = "text/plain")
    public ResponseEntity<String> setTemplateContent(@PathVariable String name, @RequestBody String content) {
        try {
            Document.OutputSettings settings = new Document.OutputSettings();
            settings.prettyPrint(false);
            String strippedHtmlSyntax = Jsoup.clean(content, "", Whitelist.none(), settings);
            templateManagementService.writeToFile(name, strippedHtmlSyntax);
            return new ResponseEntity<>(templateManagementService.getCompiledTemplate(name), HttpStatus.OK);
        } catch (IOException e) {
            System.out.println("Klarte ikke å skrive til fil");
            e.printStackTrace();
        }
        return null;
    }
}
