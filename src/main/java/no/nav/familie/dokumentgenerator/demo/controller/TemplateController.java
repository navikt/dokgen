package no.nav.familie.dokumentgenerator.demo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.jknack.handlebars.Template;
import no.nav.familie.dokumentgenerator.demo.model.TemplateService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URL;
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
            return templateManagementService.listAllTemplateNames();
        } catch (IOException e) {
            System.out.println("Kunne ikke finne noen maler!");
            e.printStackTrace();
        }
        return null;
    }

//    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping(value = "maler/{name}", produces="text/html")
    public String getTemplate(@PathVariable String name) {
        try {
            Template template = templateManagementService.getTemplate(name);
            URL path = templateManagementService.getJsonPath(name + ".json");
            JsonNode jsonNode = templateManagementService.readJsonFile(path);
//            return template;
            System.out.println("template.apply(templateManagementService.getContext(jsonNode)) = " + template.apply(templateManagementService.getContext(jsonNode)));
            return template.apply(templateManagementService.getContext(jsonNode));
        } catch (IOException | com.github.jknack.handlebars.HandlebarsException e) {
            e.printStackTrace();
        }
        return null;
    }

}
