package no.nav.familie.dokumentgenerator.dokgen.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.JsonNodeValueResolver;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.context.FieldValueResolver;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.context.MethodValueResolver;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import no.nav.familie.dokumentgenerator.dokgen.util.MalUtil;
import org.everit.json.schema.ValidationException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
public class TemplateService {
    private static final Logger LOG = LoggerFactory.getLogger(TemplateService.class);

    private Handlebars handlebars;
    private DokumentGeneratorService dokumentGeneratorService;
    private JsonService jsonService;

    private Path contentRoot;

    @Autowired
    TemplateService(@Value("${path.content.root:./content/}") Path contentRoot, DokumentGeneratorService dokumentGeneratorService, JsonService jsonService) {
        this.contentRoot = contentRoot;
        TemplateLoader loader = new FileTemplateLoader(MalUtil.hentMalRoot(contentRoot).toFile());
        handlebars = new Handlebars(loader);
        this.dokumentGeneratorService = dokumentGeneratorService;
        this.jsonService = jsonService;
    }

    private Template kompilerMal(String malNavn) {
        try {
            return handlebars.compile(malNavn + "/" + malNavn);
        } catch (IOException e) {
            LOG.error("Kompilering av templates feiler", e);
        }
        return null;
    }

    private String hentKompilertMal(String malNavn, JsonNode interleavingFields) throws ValidationException, IOException {
        Template template = kompilerMal(malNavn);

        jsonService.validateTestData(MalUtil.hentJsonSchemaForMal(contentRoot, malNavn), interleavingFields.toString());
        if (template != null) {
            return template.apply(insertTestData(interleavingFields));
        }
        return null;
    }

    private Context insertTestData(JsonNode model) {
        return Context
                .newBuilder(model)
                .resolver(JsonNodeValueResolver.INSTANCE,
                        JavaBeanValueResolver.INSTANCE,
                        FieldValueResolver.INSTANCE,
                        MapValueResolver.INSTANCE,
                        MethodValueResolver.INSTANCE
                ).build();
    }


    public List<String> hentAlleMaler() {
        try (Stream<Path> paths = Files.list(MalUtil.hentMalRoot(contentRoot))) {
            return paths
                    .filter(Files::isDirectory)
                    .map(x -> x.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOG.error("Kan ikke hente maler i contentRoot={}", contentRoot, e);
            return new ArrayList<>();
        }
    }

    public String hentMal(String malNavn) {
        try {
            return new String(Files.readAllBytes(MalUtil.hentMal(contentRoot, malNavn)));
        } catch (IOException e) {
            LOG.error("Kan ikke hente mal={}", malNavn, e);
        }
        return null;
    }

    public byte[] lagPdf(String malNavn, String payload) {
        try {
            JsonNode jsonContent = jsonService.getJsonFromString(payload);

            JsonNode valueFields = jsonService.extractInterleavingFields(
                    malNavn,
                    jsonContent,
                    jsonContent.get("useTestSet").asBoolean()
            );
            return konverterBrevTilPdf(malNavn, valueFields);
        } catch (IOException e) {
            LOG.error("Feil ved henting av brev respons", e);
            throw new RuntimeException("Kunne ikke lage pdf, malNavn={} " + malNavn, e);
        }
    }

    public String lagHtml(String malNavn, String payload) {
        try {
            JsonNode jsonContent = jsonService.getJsonFromString(payload);

            JsonNode valueFields = jsonService.extractInterleavingFields(
                    malNavn,
                    jsonContent,
                    jsonContent.get("useTestSet").asBoolean()
            );

            return konverterBrevTilHtml(malNavn, valueFields);
        } catch (IOException e) {
            LOG.error("Feil ved henting av brev respons", e);
            throw new RuntimeException("Kunne ikke lage pdf, malNavn={} " + malNavn, e);
        }
    }

    public void lagreMal(String malNavn, String payload) {
        try {
            JsonNode jsonContent = jsonService.getJsonFromString(payload);

            Document.OutputSettings settings = new Document.OutputSettings();
            settings.prettyPrint(false);
            String markdownContent = jsonContent.get("markdownContent").textValue();

            String strippedHtmlSyntax = Jsoup.clean(
                    markdownContent,
                    "",
                    Whitelist.none(),
                    settings
            );

            String path = "templates/" + malNavn + "/" + malNavn + ".hbs";

            Path newFilePath = contentRoot.resolve(path);
            Files.write(newFilePath, strippedHtmlSyntax.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new RuntimeException("Feil ved lagring av mal=" + malNavn + " payload=" + payload);
        }
    }

    private String konverterBrevTilHtml(String malNavn, JsonNode interleavingFields) {
        String compiledTemplate;

        try {
            compiledTemplate = hentKompilertMal(malNavn, interleavingFields);
        } catch (IOException e) {
            throw new RuntimeException("Ukjent feil ved konvertering av brev mal={}" + malNavn, e);
        }

        Document styledHtml = dokumentGeneratorService.appendHtmlMetadata(compiledTemplate, "html");
        return styledHtml.html();
    }

    private byte[] konverterBrevTilPdf(String malNavn, JsonNode interleavingFields) {
        String compiledTemplate;

        try {
            compiledTemplate = hentKompilertMal(malNavn, interleavingFields);
        } catch (IOException e) {
            throw new RuntimeException("Ukjent feil ved konvertering av brev mal={}" + malNavn, e);
        }
        Document styledHtml = dokumentGeneratorService.appendHtmlMetadata(compiledTemplate, "pdf");
        dokumentGeneratorService.addDocumentParts(styledHtml);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        dokumentGeneratorService.genererPDF(styledHtml, outputStream);
        return outputStream.toByteArray();
    }
}