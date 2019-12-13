package no.nav.dokgen.controller;


import io.swagger.annotations.ApiOperation;
import no.nav.dokgen.resources.TemplateResource;
import no.nav.dokgen.resources.TestDataResource;
import no.nav.dokgen.services.*;
import no.nav.dokgen.util.DocFormat;
import no.nav.dokgen.util.HttpUtil;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
public class TemplateController {


    @Value("${write.access:false}")
    private Boolean writeAccess;
    private final TemplateService templateService;
    private final TestDataService testdataService;
    private final JsonService jsonService;
    private final DocumentGeneratorService documentGeneratorService;

    public TemplateController(TemplateService templateService, TestDataService testdataService, JsonService jsonService,
                              DocumentGeneratorService documentGeneratorService) {
        this.templateService = templateService;
        this.testdataService = testdataService;
        this.jsonService = jsonService;
        this.documentGeneratorService = documentGeneratorService;
    }

    @GetMapping("/templates")
    @ApiOperation(value = "Get a list over all templates availiable")
    @ResponseStatus(HttpStatus.OK)
    public List<Resource<TemplateResource>> listTemplates() {
        List<Resource<TemplateResource>> templates = new ArrayList<>();
        for (var templateName : templateService.listTemplates()) {
            templates.add(HateoasService.templateLinks(templateName));
        }
        return templates;
    }

    @GetMapping(value = "/template/{templateName}/testdata")
    @ApiOperation(value = "Hent de forskjellige testdataene for spesifikk mal")
    public List<Resource<TestDataResource>> listTestData(@PathVariable String templateName) {
        List<Resource<TestDataResource>> testData = new ArrayList<>();
        for (var testDataName : testdataService.listTestData(templateName)) {
            testData.add(HateoasService.testDataLinks(templateName, testDataName));
        }
        return testData;
    }

    @GetMapping(value = "/template/{templateName}/markdown", produces = "text/plain")
    @ApiOperation(value = "Hent malen i markdown")
    public ResponseEntity<String> getTemplateAsMarkdown(@PathVariable String templateName) {
        var responseBody = templateService.getTemplate(templateName);
        return new ResponseEntity<>(responseBody.getContent(), HttpStatus.OK);
    }

    @PostMapping(value = "/template/{templateName}/create-pdf", consumes = "application/json")
    @ApiOperation(value = "Lager en PDF av flettefeltene og malen.", notes = "PDF er av versjonen PDF/A")
    public ResponseEntity createPdf(@PathVariable String templateName, @RequestBody String mergeFields) {
        byte[] pdf = templateService.createPdf(templateName, mergeFields);
        return new ResponseEntity<>(pdf, HttpUtil.genHeaders(DocFormat.PDF, templateName, false), HttpStatus.OK);
    }

    @PostMapping(value = "/template/{templateName}/create-html", consumes = "application/json")
    @ApiOperation(value = "Lager en HTML av flettefeltene og malen.", notes = "")
    public ResponseEntity createHtml(@PathVariable String templateName, @RequestBody String mergeFields) {
        String html = templateService.createHtml(templateName, mergeFields);
        return new ResponseEntity<>(html, HttpUtil.genHeaders(DocFormat.HTML, templateName, false), HttpStatus.OK);
    }

    @PostMapping(value = "/template/{templateName}/create-markdown", consumes = "application/json", produces = "text/plain")
    @ApiOperation(value = "Lager Markdown av flettefeltene og malen.", notes = "")
    public ResponseEntity createMarkdown(@PathVariable String templateName, @RequestBody String mergefields) {
        String markdown = templateService.createMarkdown(templateName, mergefields);
        return new ResponseEntity<>(markdown, HttpStatus.OK);
    }

    @PostMapping(value = "/template/markdown/to-html", consumes = "text/plain")
    @ApiOperation(value = "Konverterer markdown til HTML.", notes = "")
    public ResponseEntity createHtmlCustom(@RequestBody String markdownContent) {
        var content = documentGeneratorService.appendHtmlMetadata(markdownContent, DocFormat.HTML);
        return new ResponseEntity<>(content.html(), HttpUtil.genHtmlHeaders(), HttpStatus.OK);
    }

    @GetMapping(value = "/template/{templateName}/preview-pdf/{testDataName}")
    @ApiOperation(value = "Generer malen som PDF med test data")
    public ResponseEntity previewPdf(@PathVariable String templateName, @PathVariable String testDataName) {
        String mergeFields = testdataService.getTestData(templateName, testDataName);
        return createPdf(templateName, mergeFields);
    }

    @GetMapping(value = "/template/{templateName}/preview-html/{testDataName}")
    @ApiOperation(value = "Generer malen som HTML med test data")
    public ResponseEntity previewHtml(@PathVariable String templateName, @PathVariable String testDataName) {
        String mergeFields = testdataService.getTestData(templateName, testDataName);
        return createHtml(templateName, mergeFields);
    }

    @PostMapping(value = "/template/{templateName}/preview-pdf/{testDataName}")
    @ApiOperation(value = "Generer malen som PDF med test data")
    public ResponseEntity previewPdfCustom(@PathVariable String templateName, @PathVariable String testDataName, @RequestBody String templateContent) {
        String mergeFields = testdataService.getTestData(templateName, testDataName);
        TemplateResource templateResource = new TemplateResource(templateName);
        templateResource.setContent(templateContent);
        byte[] pdfContent = templateService.createPdf(templateResource, mergeFields);
        return new ResponseEntity<>(pdfContent, HttpUtil.genHeaders(DocFormat.PDF, templateName, false), HttpStatus.OK);
    }

    @PostMapping(value = "/template/{templateName}/preview-html/{testDataName}")
    @ApiOperation(value = "Generer malen som HTML med test data")
    public ResponseEntity previewHtmlCustom(@PathVariable String templateName, @PathVariable String testDataName, @RequestBody String templateContent) {
        String mergeFields = testdataService.getTestData(templateName, testDataName);
        TemplateResource templateResource = new TemplateResource(templateName);
        templateResource.setContent(templateContent);
        String htmlContent = templateService.createHtml(templateResource, mergeFields);
        return new ResponseEntity<>(htmlContent, HttpUtil.genHeaders(DocFormat.PDF, templateName, false), HttpStatus.OK);
    }


    @PostMapping(value = "/template/{templateName}/markdown", consumes = "application/json")
    public ResponseEntity upsertTemplate(@PathVariable String templateName, @RequestBody String payload) {
        if (!writeAccess) {
            return new ResponseEntity(HttpStatus.FORBIDDEN);
        }
        templateService.saveTemplate(templateName, payload);
        return new ResponseEntity<>(HttpStatus.OK);
    }


    @GetMapping(value = "/template/{templateName}/schema", produces = "application/json")
    @ApiOperation(value = "Returnerer json schema for malen.")
    public ResponseEntity<String> getSchema(@PathVariable String templateName) {
        return new ResponseEntity<>(jsonService.getSchemaAsString(templateName), HttpStatus.OK);
    }

    @PostMapping(value = "/template/{templateName}/testdata/{testDataName}", consumes = "application/json", produces = "application/json")
    @ApiOperation(
            value = "Oppdaterer eller lager et nytt testsett for en mal",
            notes = "For å generere et nytt testsett poster du testdataene på endepunktet."
    )
    public ResponseEntity upsertTestData(
            @PathVariable String templateName,
            @PathVariable String testDataName,
            @RequestBody String payload) {
        if (!writeAccess) {
            return new ResponseEntity(HttpStatus.FORBIDDEN);
        }
        String testSetName = testdataService.saveTestData(templateName, testDataName, payload);
        return new ResponseEntity<>(testSetName, HttpStatus.CREATED);
    }

    @PostMapping(value = "/template/{templateName}/download-pdf", consumes = "application/json")
    @ApiOperation(value = "Last ned et brev i PDF/A-format", notes = "")
    public ResponseEntity downloadPdf(@PathVariable String templateName, @RequestBody String payload) {
        byte[] pdf = templateService.createPdf(templateName, payload);
        return new ResponseEntity<>(pdf, HttpUtil.genHeaders(DocFormat.PDF, templateName, true), HttpStatus.OK);
    }
}