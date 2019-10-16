package no.nav.dokgen.services;

import no.nav.dokgen.controller.TemplateController;
import no.nav.dokgen.resources.TemplateResource;
import no.nav.dokgen.resources.TestDataResource;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Service;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Service
public class HateoasService {
    static public Resource<TemplateResource> templateLinks(String templateName) {
        return new Resource<>(new TemplateResource(templateName),
                linkTo(methodOn(TemplateController.class).getTemplateAsMarkdown(templateName)).withSelfRel(),
                linkTo(methodOn(TemplateController.class).listTestData(templateName)).withRel("testdatas"),
                linkTo(methodOn(TemplateController.class).getSchema(templateName)).withRel("schema"),
                linkTo(methodOn(TemplateController.class).createPdf(templateName,"{}")).withRel("create-pdf"),
                linkTo(methodOn(TemplateController.class).createHtml(templateName,"{}")).withRel("create-html")
        );
    }

    static public Resource<TestDataResource> testDataLinks(String templateName, String testDataName) {
        return new Resource<>(new TestDataResource(templateName, testDataName),
                linkTo(methodOn(TemplateController.class).previewHtml(templateName, testDataName)).withRel("preview-html"),
                linkTo(methodOn(TemplateController.class).previewPdf(templateName, testDataName)).withRel("preview-pdf")
        );
    }
}
