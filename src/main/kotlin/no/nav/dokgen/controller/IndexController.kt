package no.nav.dokgen.controller

import no.nav.dokgen.resources.IndexResource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.hateoas.Link
import org.springframework.hateoas.Resource
import org.springframework.hateoas.mvc.ControllerLinkBuilder
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import springfox.documentation.swagger2.web.Swagger2Controller
import javax.servlet.http.HttpServletRequest

@RestController
class IndexController(
    val servletRequest: HttpServletRequest
) {
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = ["/"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun listTemplates(): Resource<IndexResource> {
        val link = Link(servletRequest.requestURL.toString() + "swagger-ui.html")
            .withRel("swagger-ui")
        return Resource(
            IndexResource("dokgen"),
            ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(TemplateController::class.java).listTemplates())
                .withRel("templates"),
            link,
            ControllerLinkBuilder.linkTo(
                ControllerLinkBuilder.methodOn(Swagger2Controller::class.java)
                    .getDocumentation("default", servletRequest)
            ).withRel("swagger-doc")
        )
    }
}