package no.nav.familie.dokgen.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.JsonNodeValueResolver;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.context.FieldValueResolver;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.context.MethodValueResolver;
import com.github.jknack.handlebars.helper.ConditionalHelpers;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import no.nav.familie.dokgen.feil.DokgenIkkeFunnetException;
import no.nav.familie.dokgen.util.MalUtil;
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
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
public class TemplateService {
    private static final Logger LOG = LoggerFactory.getLogger(TemplateService.class);

    private final Handlebars handlebars;
    private final DokumentGeneratorService dokumentGeneratorService;
    private final JsonService jsonService;

    private final Path contentRoot;

    @Autowired
    TemplateService(@Value("${path.content.root:./content/}") Path contentRoot, DokumentGeneratorService dokumentGeneratorService, JsonService jsonService) {
        this.contentRoot = contentRoot;
        TemplateLoader loader = new FileTemplateLoader(MalUtil.hentMalRoot(contentRoot).toFile());
        handlebars = new Handlebars(loader);
        handlebars.registerHelper("eq", ConditionalHelpers.eq);
        handlebars.registerHelper("neq", ConditionalHelpers.neq);
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

        jsonService.validereJson(MalUtil.hentJsonSchemaForMal(contentRoot, malNavn), interleavingFields.toString());
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
            return new ArrayList<>();
        }
    }

    public String hentMal(String malNavn) {
        try {
            return Files.readString(MalUtil.hentMal(contentRoot, malNavn), StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            throw new DokgenIkkeFunnetException("Kan ikke finne mal med navn " + malNavn);
        } catch (IOException e) {
            throw new RuntimeException("Kan ikke hente mal " + malNavn, e);
        }
    }

    public byte[] lagPdf(String malNavn, String payload) {
        try {
            JsonNode jsonContent = jsonService.getJsonFromString(payload);

            JsonNode valueFields = jsonService.extractInterleavingFields(
                    malNavn,
                    jsonContent,
                    jsonContent.get("useTestSet") != null && jsonContent.get("useTestSet").asBoolean()
            );
            return konverterBrevTilPdf(malNavn, valueFields);
        } catch (IOException e) {
            throw new RuntimeException("Kunne ikke lage pdf, malNavn={} " + malNavn, e);
        }
    }

    public String lagHtml(String malNavn, String payload) {
        try {
            JsonNode jsonContent = jsonService.getJsonFromString(payload);

            JsonNode valueFields = jsonService.extractInterleavingFields(
                    malNavn,
                    jsonContent,
                    jsonContent.get("useTestSet") != null && jsonContent.get("useTestSet").asBoolean()
            );

            return konverterBrevTilHtml(malNavn, valueFields);
        } catch (IOException e) {
            throw new RuntimeException("Kunne ikke lage pdf, malNavn={} " + malNavn, e);
        }
    }

    public void lagreMal(String malNavn, String payload) {
        try {
            JsonNode jsonContent = jsonService.getJsonFromString(payload);

            Document.OutputSettings settings = new Document.OutputSettings();
            settings.prettyPrint(false);
            JsonNode markdownContent1 = jsonContent.get("markdownContent");
            if (markdownContent1 != null) {
                String markdownContent = jsonContent.get("markdownContent").textValue();
                String strippedHtmlSyntax = Jsoup.clean(
                        markdownContent,
                        "",
                        Whitelist.none(),
                        settings
                );

                Path malPath = MalUtil.hentMal(contentRoot, malNavn);
                Files.createDirectories(malPath.getParent());

                Files.write(malPath, strippedHtmlSyntax.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
            } else {
                throw new IllegalArgumentException("Kan ikke hente markdown for payload=" + payload);
            }
        } catch (IOException e) {
            throw new RuntimeException("Feil ved lagring av mal=" + malNavn + " payload=" + payload, e);
        }
    }

    private String konverterBrevTilHtml(String malNavn, JsonNode interleavingFields) {
        String compiledTemplate;

        try {
            compiledTemplate = hentKompilertMal(malNavn, interleavingFields);
        } catch (IOException e) {
            throw new RuntimeException("Ukjent feil ved konvertering av brev mal=" + malNavn, e);
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