package no.nav.dokgen.controller

import jakarta.servlet.http.HttpServletRequest
import no.nav.dokgen.resources.IndexResource
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.Link
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class IndexController(
    val servletRequest: HttpServletRequest
) {
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = ["/"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun listTemplates(): EntityModel<IndexResource> {
        val link = Link.of(servletRequest.requestURL.toString() + "swagger-ui/index.html")
            .withRel("swagger-ui")
        return EntityModel.of(
            IndexResource("dokgen"),
            linkTo(methodOn(TemplateController::class.java).listTemplates())
                .withRel("templates"),
            link
        )
    }
}
