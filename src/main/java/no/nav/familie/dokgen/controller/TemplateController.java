package no.nav.familie.dokgen.controller;


import io.swagger.annotations.ApiOperation;
import no.nav.familie.dokgen.services.TestdataService;

import no.nav.familie.dokgen.services.TemplateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@CrossOrigin(origins = {"http://localhost:3000"})
@RestController
public class TemplateController {

    @Value("${write.access:false}")
    private Boolean writeAccess;


    private final TemplateService malService;
    private final TestdataService testdataService;

    public TemplateController(TemplateService malService, TestdataService testdataService) {
        this.malService = malService;
        this.testdataService = testdataService;
    }

    @GetMapping("/mal/alle")
    @ApiOperation(value = "Få en liste over alle malene som er tilgjengelig")
    public List<String> hentAlleMaler() {
        return malService.hentAlleMaler();
    }

    @GetMapping(value = "/mal/{malNavn}", produces = "text/plain")
    @ApiOperation(value = "Hent malen i markdown")
    public String hentMal(@PathVariable String malNavn) {
        return malService.hentMal(malNavn);
    }

    @PostMapping(value = "/mal/{format}/{malNavn}", consumes = "application/json")
    @ApiOperation(
            value = "Generer malen i ønsket format",
            notes = "Støttede formater er <b>html</b> og <b>pdf</b>, hvor PDF er av versjonen PDF/A"
    )
    public ResponseEntity genererMal(@PathVariable String format,
                                     @PathVariable String malNavn,
                                     @RequestBody String payload) {
        if ("pdf".equals(format)) {
            byte[] pdf = malService.lagPdf(malNavn, payload);
            return new ResponseEntity<>(pdf, genHeaders(format, malNavn, false), HttpStatus.OK);
        } else if ("html".equals(format)) {
            String html = malService.lagHtml(malNavn, payload);
            return new ResponseEntity<>(html, genHeaders(format, malNavn, false), HttpStatus.OK);
        } else {
            throw new IllegalArgumentException("Ukjent format " + format);
        }
    }

    private HttpHeaders genHeaders(String format, String malNavn, boolean download) {
        if (format.equals("html")) {
            return genHtmlHeaders();
        } else if (format.equals("pdf")) {
            return genPdfHeaders(malNavn, download);
        }
        return null;
    }

    private HttpHeaders genHtmlHeaders() {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.put("charset", Collections.singletonList(StandardCharsets.UTF_8.toString()));
        HttpHeaders headers = new HttpHeaders(map);
        headers.setContentType(MediaType.TEXT_HTML);
        return headers;
    }

    private HttpHeaders genPdfHeaders(String malNavn, boolean download) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        String filename = malNavn + ".pdf";
        headers.setContentDispositionFormData(download ? "attachment" : "inline", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        return headers;
    }

    @PutMapping(value = "/mal/{format}/{malNavn}", consumes = "application/json")
    public ResponseEntity endreMalInnhold(@PathVariable String format,
                                          @PathVariable String malNavn,
                                          @RequestBody String payload) {
        if (!writeAccess) {
            return new ResponseEntity(HttpStatus.FORBIDDEN);
        }
        malService.lagreMal(
                malNavn,
                payload
        );

        return this.genererMal(format, malNavn, payload);
    }

    @GetMapping(value = "mal/{malNavn}/testdata")
    @ApiOperation(value = "Hent de forskjellige testdataene for spesifikk mal")
    public ResponseEntity<List<String>> hentTestdataForMal(@PathVariable String malNavn) {
        return new ResponseEntity<>(testdataService.hentTestdatasettForMal(malNavn), HttpStatus.OK);
    }

    @GetMapping(value = "mal/{malNavn}/tomtTestSett", produces = "application/json")
    @ApiOperation(value = "Hent et tomt testsett for malen som kan fylles ut")
    public ResponseEntity<String> hentTomtTestdataSett(@PathVariable String malNavn) {
        return new ResponseEntity<>(testdataService.hentTomtTestsett(malNavn), HttpStatus.OK);
    }

    @PostMapping(value = "mal/{malNavn}/nyttTestSett", consumes = "application/json", produces = "application/json")
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
    public ResponseEntity lagreNyttTestset(@PathVariable String malNavn, @RequestBody String payload) {
        String testSetName = testdataService.lagTestsett(malNavn, payload);
        return new ResponseEntity<>(testSetName, HttpStatus.CREATED);
    }

    @PostMapping(value = "/brev/{malNavn}/download", consumes = "application/json")
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
                    "}")
    public ResponseEntity hentPdf(@PathVariable String malNavn,
                                  @RequestBody String payload) {
        byte[] pdf = malService.lagPdf(malNavn, payload);
        return new ResponseEntity<>(pdf, genHeaders("pdf", malNavn, true), HttpStatus.OK);
    }
}