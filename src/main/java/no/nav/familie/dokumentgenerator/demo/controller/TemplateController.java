package no.nav.familie.dokumentgenerator.demo.controller;

import no.nav.familie.dokumentgenerator.demo.model.TemplateService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
        return templateManagementService.getTemplateSuggestions();
    }

    @GetMapping(value = "maler/markdown/{name}", produces = "text/plain")
    public String getTemplateContentInMarkdown(@PathVariable String name) {
        return templateManagementService.getCompiledTemplate(name);
    }

    @GetMapping(value = "maler/html/{name}", produces = "text/html")
    public String getTemplateContentInHtml(@PathVariable String name) {
            String compiledMarkdownTemplate = templateManagementService.getCompiledTemplate(name);

            if (compiledMarkdownTemplate == null) {
                return null;
            }

            String html = templateManagementService.convertMarkdownTemplateToHtml(compiledMarkdownTemplate);

            Document document = Jsoup.parse(html);
            Element head = document.head();
            head.append("<meta charset=\"UTF-8\">");
            head.append(("<link rel=\"stylesheet\" href=\"css/main.css\">"));
            return templateManagementService.convertMarkdownTemplateToHtml(document.html());
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

    @GetMapping(value = "maler/pdf/{templateName}", produces = "application/pdf")
    public ResponseEntity<byte[]> getPDF(@PathVariable String templateName) {
        byte[] pdfContent = templateManagementService.generatePDF(templateName);

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

}
