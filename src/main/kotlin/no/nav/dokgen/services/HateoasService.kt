package no.nav.dokgen.services

import no.nav.dokgen.controller.TemplateController
import no.nav.dokgen.resources.TemplateResource
import no.nav.dokgen.resources.TestDataResource
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn

object HateoasService {
    fun templateLinks(templateName: String): EntityModel<TemplateResource> {
        return EntityModel.of(TemplateResource(templateName),
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

    fun testDataLinks(templateName: String, testDataName: String): EntityModel<TestDataResource> {
        return EntityModel.of(
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