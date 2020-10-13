package no.nav.dokgen.services

import no.nav.dokgen.controller.TemplateController
import no.nav.dokgen.resources.TemplateResource
import no.nav.dokgen.resources.TestDataResource
import org.springframework.hateoas.Resource
import org.springframework.hateoas.mvc.ControllerLinkBuilder
import org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo
import org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn

object HateoasService {
    fun templateLinks(templateName: String): Resource<TemplateResource> {
        return Resource(
            TemplateResource(templateName),
            linkTo(
                methodOn(TemplateController::class.java).getTemplateAsMarkdown(templateName)
            ).withSelfRel(),
            linkTo(
                methodOn(TemplateController::class.java).listTestData(templateName)
            ).withRel("testdatas"),
            linkTo(
                methodOn(TemplateController::class.java).getSchema(templateName)
            ).withRel("schema"),
            linkTo(
                methodOn(TemplateController::class.java).createPdf(templateName, "{}")
            ).withRel("create-pdf"),
            linkTo(
                methodOn(TemplateController::class.java).createHtml(templateName, "{}")
            ).withRel("create-html")
        )
    }

    fun testDataLinks(templateName: String, testDataName: String): Resource<TestDataResource> {
        return Resource(
            TestDataResource(templateName, testDataName),
            linkTo(
                methodOn(TemplateController::class.java).previewHtml(templateName, testDataName)
            ).withRel("preview-html"),
            linkTo(
                methodOn(TemplateController::class.java).previewPdf(templateName, testDataName)
            ).withRel("preview-pdf")
        )
    }
}