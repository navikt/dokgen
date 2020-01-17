package no.nav.dokgen.controller;

import no.nav.dokgen.resources.IndexResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.swagger2.web.Swagger2Controller;

import javax.servlet.http.HttpServletRequest;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
public class IndexController {

    @Autowired
    private HttpServletRequest servletRequest;

    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public Resource<IndexResource> listTemplates() {

        var link = (new Link(servletRequest.getRequestURL().toString() + "swagger-ui.html"))
                .withRel("swagger-ui");
        return new Resource<>(new IndexResource("dokgen"),
                linkTo(methodOn(TemplateController.class).listTemplates()).withRel("templates"),
                link,
                linkTo(methodOn(Swagger2Controller.class).getDocumentation("default", servletRequest)).withRel("swagger-doc")
        );
    }
}
