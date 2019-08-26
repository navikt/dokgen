package no.nav.familie.dokgen.controller;


import io.swagger.annotations.ApiOperation;
import no.nav.familie.dokgen.feil.DokgenValideringException;
import no.nav.familie.dokgen.services.TestdataService;

import no.nav.familie.dokgen.services.TemplateService;
import org.everit.json.schema.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger LOG = LoggerFactory.getLogger(TemplateController.class);

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
    public ResponseEntity setTemplateContent(@PathVariable String format,
                                             @PathVariable String malNavn,
                                             @RequestBody String payload) {
        LOG.info("Genererer mal i ønsket format. Format={}, malNavn={}, payload={}", format, malNavn, payload);
        Object dokument;
        try {
            if ("pdf".equals(format)) {
                dokument = malService.lagPdf(malNavn, payload);
            } else if ("html".equals(format)) {
                dokument = malService.lagHtml(malNavn, payload);
            } else {
                return new ResponseEntity<>("Ukjent format " + format, HttpStatus.BAD_REQUEST);
            }
        } catch (ValidationException e) {
            return new ResponseEntity<>(e.toJSON().toString(), HttpStatus.BAD_REQUEST);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (dokument == null) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        } else {
            return new ResponseEntity<>(dokument, genHeaders(format, malNavn, false), HttpStatus.OK);
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
        try {
            malService.lagreMal(
                    malNavn,
                    payload
            );
        } catch (RuntimeException e) {
            LOG.error("Feil ved endring av mal={}", malNavn, e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return this.setTemplateContent(format, malNavn, payload);
    }

    @GetMapping(value = "mal/{malNavn}/testdata")
    @ApiOperation(value = "Hent de forskjellige testdataene for spesifikk mal")
    public ResponseEntity<List<String>> hentTestdataForMal(@PathVariable String malNavn) {
        List<String> response = testdataService.hentTestdatasettForMal(malNavn);

        if (response == null) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

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
        String testSetName = null;
        try {
            testSetName = testdataService.lagTestsett(malNavn, payload);
            return new ResponseEntity<>(testSetName, HttpStatus.CREATED);
        } catch (DokgenValideringException e) {
            LOG.info("Valideringsfeil ved lagring av nytt testdata for mal={}", malNavn);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (RuntimeException e) {
            LOG.error("Ukjent feil ved lagring av nytt testdata for mal={}", malNavn, e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
        try {
            byte[] pdf = malService.lagPdf(malNavn, payload);
            if (pdf == null) {
                return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
            } else {
                return new ResponseEntity<>(pdf, genHeaders("pdf", malNavn, true), HttpStatus.OK);
            }

        } catch (ValidationException e) {
            LOG.info("Valideringsfeil ved henting av pdf for mal={}", malNavn);
            return new ResponseEntity<>(e.toJSON().toString(), HttpStatus.BAD_REQUEST);
        } catch (RuntimeException e) {
            LOG.error("Ukjent feil ved henting av pdf for mal={}", malNavn, e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
