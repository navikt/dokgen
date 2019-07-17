package no.nav.familie.dokumentgenerator.demo.controller;

import no.nav.familie.dokumentgenerator.demo.model.TemplateService;
import org.json.JSONObject;
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

    @GetMapping("/mal/alle")
    public List<String> getAllTemplateNames() {
        return templateManagementService.getTemplateSuggestions();
    }

    @GetMapping(value = "/mal", produces = "text/plain")
    public String getTemplateContentInMarkdown(@RequestParam String templateName) {
        //TODO: Make this return none-compiled template
        return templateManagementService.getCompiledTemplate(templateName, new JSONObject());
    }

    @PostMapping(value = "/mal", consumes = "text/plain")
    public ResponseEntity<String> setTemplateContent(@RequestBody String templateName,
                                                     @RequestBody String markdownContent,
                                                     @RequestBody JSONObject interleavingFields,
                                                     @RequestBody String format) {
        //TODO: Make this return compiled template with format specified
        try {
            Document.OutputSettings settings = new Document.OutputSettings();
            settings.prettyPrint(false);
            String strippedHtmlSyntax = Jsoup.clean(markdownContent, "", Whitelist.none(), settings);
            templateManagementService.writeToFile(templateName, strippedHtmlSyntax);
            return new ResponseEntity<>(
                    templateManagementService.getCompiledTemplate(templateName, interleavingFields),
                    HttpStatus.OK
            );
        } catch (IOException e) {
            System.out.println("Klarte ikke å skrive til fil");
            e.printStackTrace();
        }
        return null;
    }

    @PostMapping(value = "/brev", produces = "text/html")
    public String getTemplateContentInHtml(@RequestBody String templateName,
                                           @RequestBody JSONObject interleavingFields,
                                           @RequestBody String format) {
        //TODO: Make this return either PDF or HTML based on format
        String compiledMarkdownTemplate = templateManagementService.getCompiledTemplate(
                templateName, interleavingFields
        );

        if (compiledMarkdownTemplate == null) {
            return null;
        }

        String html = templateManagementService.convertMarkdownTemplateToHtml(compiledMarkdownTemplate, format);

        Document document = Jsoup.parse(html);
        Element head = document.head();
        head.append("<meta charset=\"UTF-8\">");
        head.append(("<link rel=\"stylesheet\" href=\"css/main.css\">"));
        return templateManagementService.convertMarkdownTemplateToHtml(document.html(), format);
    }

    //TODO: Remove, implement dynamically into letter converter
    @GetMapping(value = "maler/pdf/{templateName}", produces = "application/pdf")
    public ResponseEntity<byte[]> getPDF(@PathVariable String templateName) {
        byte[] pdfContent = templateManagementService.generatePDF(templateName, new JSONObject(), "html");

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
