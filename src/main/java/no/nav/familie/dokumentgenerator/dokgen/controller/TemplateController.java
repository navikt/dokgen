package no.nav.familie.dokumentgenerator.dokgen.controller;


import io.swagger.annotations.ApiOperation;
import org.json.JSONObject;

import no.nav.familie.dokumentgenerator.dokgen.services.TemplateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = {"http://localhost:3000"})
@RestController
public class TemplateController {

    private final TemplateService templateManagementService;

    public TemplateController(TemplateService templateManagementService) {
        this.templateManagementService = templateManagementService;
    }

    @GetMapping("/mal/alle")
    @ApiOperation(value = "Få en liste over alle malene som er tilgjengelig")
    public List<String> getAllTemplateNames() {
        return templateManagementService.getTemplateSuggestions();
    }

    @GetMapping(value = "/mal/{templateName}", produces = "text/plain")
    @ApiOperation(value = "Hent malen i markdown")
    public String getTemplateContentInMarkdown(@PathVariable String templateName) {
        return templateManagementService.getMarkdownTemplate(templateName);
    }

    @PostMapping(value = "/mal/{format}/{templateName}", consumes = "application/json")
    @ApiOperation(
            value = "Generer malen i ønsket format",
            notes = "Støttede formater er <b>html</b> og <b>pdf</b>, hvor PDF er av versjonen PDF/A"
    )
    public ResponseEntity setTemplateContent(@PathVariable String format,
                                             @PathVariable String templateName,
                                             @RequestBody String payload) {
        return templateManagementService.returnLetterResponse(
                format,
                templateName,
                payload
        );
    }

    @PutMapping(value = "/mal/{format}/{templateName}", consumes = "application/json")
    public ResponseEntity updateTemplateContent(@PathVariable String format,
                                                @PathVariable String templateName,
                                                @RequestBody String payload) {
        return templateManagementService.saveAndReturnTemplateResponse(
                format,
                templateName,
                payload
        );
    }

    @GetMapping(value = "mal/{templateName}/testdata")
    @ApiOperation(value = "Hent de forskjellige testdataene for spesifikk mal")
    public ResponseEntity<List<String>> getTestData(@PathVariable String templateName) {
        List<String> response = templateManagementService.getTestdataNames(templateName);

        if (response == null) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(templateManagementService.getTestdataNames(templateName), HttpStatus.OK);
    }

    @GetMapping(value = "mal/{templateName}/tomtTestSett", produces = "application/json")
    @ApiOperation(value = "Hent et tomt testsett for malen som kan fylles ut")
    public ResponseEntity<String> getEmptyTestSet(@PathVariable String templateName) {
        return new ResponseEntity<>(templateManagementService.getEmptyTestSet(templateName), HttpStatus.OK);
    }


    @PostMapping(value="mal/{templateName}/nyttTestSett", consumes = "application/json", produces = "application/json")
    @ApiOperation(
            value = "Lag et nytt testsett for en mal",
            notes = "For å generere et tomt testsett må oppsettet i payloaden følge lignende struktur: \n" +
                    "{" +
                    "\n\"content\": {" +
                    "\n\"begrunnelse\": \"BEGRUNNELSE\"," +
                    "\n\"paragraf\": \"10, 11\"," +
                    "\n\"enhet\": \"ENHET\"," +
                    "\n\"saksbehandler\": \"Ola Nordmann\"\n" +
                    "},\n" +
                    "\"name\": \"Navn på testsettet\"\n" +
                    "}"
    )
    public ResponseEntity createNewTestSet(@PathVariable String templateName, @RequestBody String payload) {
        return templateManagementService.createTestSet(templateName, payload);
    }


    @PostMapping(value = "/brev/{format}/{templateName}", consumes = "application/json")
    public ResponseEntity getGeneratedContent(@PathVariable String format,
                                                   @PathVariable String templateName,
                                                   @RequestBody String payload) {
        return templateManagementService.returnLetterResponse(
                format,
                templateName,
                payload
        );
    }

    @PostMapping(value = "/brev/{templateName}/download", consumes = "application/json")
    @ApiOperation(value = "Last ned et brev i PDF/A-format",
            notes = "Dersom det er ønskelig å bruke et <b>eksisterende</b> testset må payloaden se f.eks. slik ut dersom du skal laste ned et brev for Avslag: \n" +
                    "{\n" +
                    "&nbsp;\"useTestSet\": true,\n" +
                    "\"testSetName\": \"Avslag1\"\n" +
                    "}" +
                    "\nDersom det skal flettes inn <b>ny</b> data må payloaden se f.eks. slik ut: \n" +
                    "{\n" +
                    "\"useTestSet\": false,\n" +
                    "\"interleavingFields\": {\n" +
                    "   \"begrunnelse\": \"[BEGRUNNELSE]\",\n" +
                    "   \"paragraf\": \"10, 11, 12\",\n" +
                    "   \"enhet\": \"ENHET\",\n" +
                    "   \"saksbehandler\": \"Ola Nordmann\"\n" +
                    "}\n" +
                    "}" )
    public ResponseEntity getGeneratedContentDownload(@PathVariable String templateName,
                                                      @RequestBody String payload) {
        return templateManagementService.returnLetterResponseAndDownload(
                templateName,
                payload
        );
    }
}
