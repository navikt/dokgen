package no.nav.dokgen.controller
import io.swagger.v3.oas.annotations.Operation
import no.nav.dokgen.controller.api.CreateDocumentRequest
import no.nav.dokgen.resources.TemplateResource
import no.nav.dokgen.resources.TestDataResource
import no.nav.dokgen.services.DocumentGeneratorService
import no.nav.dokgen.services.HateoasService.templateLinks
import no.nav.dokgen.services.HateoasService.testDataLinks
import no.nav.dokgen.services.JsonService
import no.nav.dokgen.services.TemplateService
import no.nav.dokgen.services.TestDataService
import no.nav.dokgen.util.DocFormat
import no.nav.dokgen.util.HttpUtil.genHeaders
import no.nav.dokgen.util.HttpUtil.genHtmlHeaders
import org.springframework.beans.factory.annotation.Value
import org.springframework.hateoas.EntityModel
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class TemplateController(
    private val templateService: TemplateService,
    private val testdataService: TestDataService,
    private val jsonService: JsonService,
    private val documentGeneratorService: DocumentGeneratorService,
    @Value("\${write.access:false}")
    private val writeAccess: Boolean
) {

    @GetMapping("/templates")
    @Operation(summary = "Get a list over all templates availiable")
    @ResponseStatus(
        HttpStatus.OK
    )
    fun listTemplates(): List<EntityModel<TemplateResource>> {
        return templateService.listTemplates().map{
            templateLinks(it)
        }
    }

    @GetMapping(value = ["/template/{templateName}/testdata"])
    @Operation(summary = "Hent de forskjellige testdataene for spesifikk mal")
    fun listTestData(@PathVariable templateName: String): List<EntityModel<TestDataResource>> {
        return testdataService.listTestData(templateName).map {
            testDataLinks(templateName, it)
        }
    }

    @GetMapping(value = ["/template/{templateName}/markdown"], produces = ["text/plain"])
    @Operation(summary = "Hent malen i markdown")
    fun getTemplateAsMarkdown(@PathVariable templateName: String): ResponseEntity<String> {
        val responseBody = templateService.getTemplate(templateName)
        return ResponseEntity(responseBody.content, HttpStatus.OK)
    }

    @PostMapping(value = ["/template/{templateName}/create-pdf"], consumes = ["application/json"])
    @Operation(summary = "Lager en PDF av flettefeltene og malen.", description = "PDF er av versjonen PDF/A")
    fun createPdf(@PathVariable templateName: String, @RequestBody mergeFields: String?): ResponseEntity<*> {
        val pdf = templateService.createPdf(templateName, mergeFields)
        return ResponseEntity(pdf, genHeaders(DocFormat.PDF, templateName, false), HttpStatus.OK)
    }

    @PostMapping(value = ["/template/create-pdf"], consumes = ["application/json"])
    @Operation(summary = "Lager en PDF av flettefeltene og malen. Støtter mal i undermapper", description = "PDF er av versjonen PDF/A")
    fun createPdfFromPath(@RequestParam("templatePath") templatePath: String, @RequestBody mergeFields: String?): ResponseEntity<*> {
        val pdf = templateService.createPdf(templatePath, mergeFields)
        return ResponseEntity(pdf, genHeaders(DocFormat.PDF, templatePath, false), HttpStatus.OK)
    }


    @PostMapping(value = ["/template/{templateName}/{variation}/create-pdf-variation"], consumes = ["application/json"])
    @Operation(
        summary = "Lager en PDF av flettefeltene og malen med angitt variation.",
        description = "PDF er av versjonen PDF/A"
    )
    fun createPdfVariation(
        @PathVariable templateName: String,
        @PathVariable variation: String,
        @RequestBody mergeFields: String
    ): ResponseEntity<*> {
        val pdf = templateService.createPdf(templateName, mergeFields, variation)
        return ResponseEntity(pdf, genHeaders(DocFormat.PDF, templateName, false), HttpStatus.OK)
    }

    @PostMapping(value = ["/template/{templateName}/{formatVariation}/create-pdf-format-variation"], consumes = ["application/json"])
    @Operation(
        summary = "Lager en PDF av flettefeltene og malen med angitt format variant.",
        description = "PDF er av versjonen PDF/A"
    )
    fun createPdfFormatVariation(
        @PathVariable templateName: String,
        @PathVariable formatVariation: DocFormat,
        @RequestBody mergeFields: String
    ): ResponseEntity<*> {
        val pdf = templateService.createPdf(templateName, mergeFields, formatVariation)
        return ResponseEntity(pdf, genHeaders(DocFormat.PDFINNTEKTSMELDING, templateName, false), HttpStatus.OK)
    }

    @PostMapping(value = ["/template/{templateName}/create-html"], consumes = ["application/json"])
    @Operation(summary = "Lager en HTML av flettefeltene og malen.", description = "")
    fun createHtml(@PathVariable templateName: String, @RequestBody mergeFields: String): ResponseEntity<*> {
        val html = templateService.createHtml(templateName, mergeFields)
        return ResponseEntity(html, genHeaders(DocFormat.HTML, templateName, false), HttpStatus.OK)
    }

    @PostMapping(
        value = ["/template/{templateName}/{variation}/create-html-variation"],
        consumes = ["application/json"]
    )
    @Operation(summary = "Lager en HTML av flettefeltene og malen med angitt variation.", description = "")
    fun createHtmlVariation(
        @PathVariable templateName: String,
        @PathVariable variation: String,
        @RequestBody mergeFields: String
    ): ResponseEntity<*> {
        val html = templateService.createHtml(templateName, mergeFields, variation)
        return ResponseEntity(html, genHeaders(DocFormat.HTML, templateName, false), HttpStatus.OK)
    }

    @PostMapping(
        value = ["/template/{templateName}/create-markdown"],
        consumes = ["application/json"],
        produces = ["text/plain"]
    )
    @Operation(summary = "Lager Markdown av flettefeltene og malen.", description = "")
    fun createMarkdown(@PathVariable templateName: String, @RequestBody mergefields: String): ResponseEntity<*> {
        val markdown = templateService.createMarkdown(templateName, mergefields)
        return ResponseEntity(markdown, HttpStatus.OK)
    }

    @PostMapping(
        value = ["/template/{templateName}/{variation}/create-markdown-variation"],
        consumes = ["application/json"],
        produces = ["text/plain"]
    )
    @Operation(summary = "Lager Markdown av flettefeltene og malen med angitt variation.", description = "")
    fun createMarkdownVariation(
        @PathVariable templateName: String,
        @PathVariable variation: String,
        @RequestBody mergefields: String
    ): ResponseEntity<*> {
        val markdown = templateService.createMarkdown(templateName, mergefields, variation)
        return ResponseEntity(markdown, HttpStatus.OK)
    }

    @Deprecated("")
    @PostMapping(value = ["/template/markdown/to-html"], consumes = ["text/markdown"])
    @Operation(summary = "Konverterer markdown til HTML.", description = "")
    fun createHtmlCustom(@RequestBody markdownContent: String): ResponseEntity<*> {
        val content = documentGeneratorService.appendHtmlMetadata(markdownContent, DocFormat.HTML)
        return ResponseEntity(content.html(), genHtmlHeaders(), HttpStatus.OK)
    }

    @PostMapping(value = ["/template/{templateName}/create-doc"], consumes = ["application/json"])
    @Operation(summary = "Lager dokument ut ifra request-objekt", description = "")
    fun createDocument(
        @PathVariable templateName: String,
        @RequestBody documentRequest: CreateDocumentRequest
    ): ResponseEntity<*> {
        val document = templateService.createDocument(documentRequest, templateName)
        return when (documentRequest.docFormat) {
            DocFormat.HTML -> ResponseEntity(
                document.html(),
                genHeaders(DocFormat.HTML, templateName, false),
                HttpStatus.OK
            )
            DocFormat.PDF -> ResponseEntity(
                templateService.generatePdf(document),
                genHeaders(DocFormat.PDF, templateName, false),
                HttpStatus.OK
            )
            else -> throw RuntimeException("Not yet implemented for CreateDocumentRequest.docFormat = " + documentRequest.docFormat.toString())
        }
    }

    @GetMapping(value = ["/template/{templateName}/preview-pdf/{testDataName}"])
    @Operation(summary = "Generer malen som PDF med test data")
    fun previewPdf(@PathVariable templateName: String, @PathVariable testDataName: String): ResponseEntity<*> {
        val mergeFields = testdataService.getTestData(templateName, testDataName)
        return createPdf(templateName, mergeFields)
    }

    @GetMapping(value = ["/template/preview-pdf"])
    @Operation(summary = "Generer malen som PDF med test data fra template som path")
    fun previewPdfFromPath(@RequestParam templatePath: String, @RequestParam testDataName: String): ResponseEntity<*> {
        val mergeFields = testdataService.getTestData(templatePath, testDataName)
        return createPdf(templatePath, mergeFields)
    }

    @GetMapping(value = ["/template/{templateName}/preview-html/{testDataName}"])
    @Operation(summary = "Generer malen som HTML med test data")
    fun previewHtml(@PathVariable templateName: String, @PathVariable testDataName: String): ResponseEntity<*> {
        val mergeFields = testdataService.getTestData(templateName, testDataName)
        return createHtml(templateName, mergeFields)
    }

    @PostMapping(value = ["/template/{templateName}/preview-pdf/{testDataName}"])
    @Operation(summary = "Generer malen som PDF med test data")
    fun previewPdfCustom(
        @PathVariable templateName: String,
        @PathVariable testDataName: String,
        @RequestBody templateContent: String
    ): ResponseEntity<*> {
        val mergeFields = testdataService.getTestData(templateName, testDataName)
        val templateResource = TemplateResource(name = templateName, content = templateContent)
        val pdfContent = templateService.createPdf(templateResource, mergeFields)
        return ResponseEntity(pdfContent, genHeaders(DocFormat.PDF, templateName, false), HttpStatus.OK)
    }

    @PostMapping(value = ["/template/{templateName}/preview-html/{testDataName}"])
    @Operation(summary = "Generer malen som HTML med test data")
    fun previewHtmlCustom(
        @PathVariable templateName: String,
        @PathVariable testDataName: String,
        @RequestBody templateContent: String
    ): ResponseEntity<*> {
        val mergeFields = testdataService.getTestData(templateName, testDataName)
        val templateResource = TemplateResource(name = templateName, content =  templateContent)
        val htmlContent = templateService.createHtml(templateResource, mergeFields)
        return ResponseEntity(htmlContent, genHeaders(DocFormat.PDF, templateName, false), HttpStatus.OK)
    }

    @PostMapping(value = ["/template/{templateName}/markdown"], consumes = ["application/json"])
    fun upsertTemplate(@PathVariable templateName: String, @RequestBody payload: String): ResponseEntity<*> {
        if (!writeAccess) {
            return ResponseEntity<Any?>(HttpStatus.FORBIDDEN)
        }
        templateService.saveTemplate(templateName, payload)
        return ResponseEntity<Any>(HttpStatus.OK)
    }

    @GetMapping(value = ["/template/{templateName}/schema"], produces = ["application/json"])
    @Operation(summary = "Returnerer json schema for malen.")
    fun getSchema(@PathVariable templateName: String): ResponseEntity<String> {
        return ResponseEntity(jsonService.getSchemaAsString(templateName), HttpStatus.OK)
    }

    @PostMapping(
        value = ["/template/{templateName}/testdata/{testDataName}"],
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    @Operation(
        summary = "Oppdaterer eller lager et nytt testsett for en mal",
        description = "For å generere et nytt testsett poster du testdataene på endepunktet."
    )
    fun upsertTestData(
        @PathVariable templateName: String,
        @PathVariable testDataName: String,
        @RequestBody payload: String
    ): ResponseEntity<*> {
        return if (!writeAccess) {
            ResponseEntity<Any?>(HttpStatus.FORBIDDEN)
        }else{
            val testSetName = testdataService.saveTestData(templateName, testDataName, payload)
            ResponseEntity(testSetName, HttpStatus.CREATED)
        }
    }

    @PostMapping(value = ["/template/{templateName}/download-pdf"], consumes = ["application/json"])
    @Operation(summary = "Last ned et brev i PDF/A-format", description = "")
    fun downloadPdf(@PathVariable templateName: String, @RequestBody payload: String): ResponseEntity<*> {
        val pdf = templateService.createPdf(templateName, payload)
        return ResponseEntity(pdf, genHeaders(DocFormat.PDF, templateName, true), HttpStatus.OK)
    }
}