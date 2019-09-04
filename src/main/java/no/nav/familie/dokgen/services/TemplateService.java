package no.nav.familie.dokgen.services;

import static no.nav.familie.dokgen.util.MalUtil.Fold;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
public class TemplateService {
    private static final Logger LOG = LoggerFactory.getLogger(TemplateService.class);

    private final Handlebars handlebars;
    private final DokumentGeneratorService dokumentGeneratorService;
    private final JsonService jsonService;

    private final Path contentRoot;

    private Map<String, Set<Fold>> filter = new HashMap<>();

    @Autowired
    TemplateService(@Value("${path.content.root:./content/}") Path contentRoot, DokumentGeneratorService dokumentGeneratorService, JsonService jsonService) {
        this.contentRoot = contentRoot;

        handlebars = Files.exists(MalUtil.hentMalRoot(contentRoot)) ? new Handlebars(new FileTemplateLoader(MalUtil.hentMalRoot(contentRoot).toFile())) : new Handlebars();
        handlebars.registerHelper("eq", ConditionalHelpers.eq);
        handlebars.registerHelper("neq", ConditionalHelpers.neq);

        this.dokumentGeneratorService = dokumentGeneratorService;
        this.jsonService = jsonService;
    }

    private Template kompilerMal(String malNavn) {
        try {
            return handlebars.compile("Fellesmal/Fellesmal");
        } catch (IOException e) {
            LOG.error("Kompilering av templates feiler", e);
        }
        return null;
    }

    private String hentKompilertMal(String malNavn, JsonNode interleavingFields, JsonNode filter) throws ValidationException, IOException {
        Template template = kompilerMal(malNavn);

        jsonService.validereJson(MalUtil.hentJsonSchemaForMal(contentRoot, malNavn), interleavingFields.toString());
        if (template != null) {
            if (filter != null && filter.size() > 0) {
                updateFilter(filter, filter.get("tab").asInt() == 0 ? malNavn : "Fellesmal");
            }
            String fulltBrev = template.apply(insertTestData(interleavingFields, malNavn));

            Stream<String> brevmalInnhold = fulltBrev.lines().takeWhile(linje -> !markererStartenPåFellesdel(linje));
            Stream<String> fellesmalInnhold = fulltBrev.lines().dropWhile(linje -> !markererStartenPåFellesdel(linje));

            var brevmalfilter = this.filter.getOrDefault(malNavn, Collections.emptySet());
            var fellesmalfilter = this.filter.getOrDefault("Fellesmal", Collections.emptySet());

            return applyFilter(brevmalfilter, brevmalInnhold).concat(applyFilter(fellesmalfilter, fellesmalInnhold));
        }

        return null;
    }

    private String applyFilter(Set<Fold> filter, Stream<String> linjer) {
        return linjer.filter(linjerFraEkskluderteAvsnitt(filter))
                .collect(Collectors.joining("\n"));
    }

    private boolean markererStartenPåFellesdel(String line) {
        return line.contains("<!--- felles start -->");
    }

    private Predicate<String> linjerFraEkskluderteAvsnitt(Set<Fold> filter) {
        var linjeNr = new AtomicInteger(0);
        return linje -> {
            boolean inkluder = filter.stream().noneMatch(fold -> fold.contains(linjeNr.get()));
            linjeNr.incrementAndGet();
            return inkluder;
        };
    }

    private void updateFilter(JsonNode filter, String malNavn) {
        if (filter.get("action") == null) {
            return;
        }
        JsonNode range = filter.get("range");
        int forsvunnedeLinjerFraMalEtterKompilering = malNavn.equals("Fellesmal") ? 1 : 0;
        Fold fold = new Fold(
                range.get("start").get("row").asInt() - forsvunnedeLinjerFraMalEtterKompilering,
                range.get("end").get("row").asInt() - forsvunnedeLinjerFraMalEtterKompilering
        );
        var kollapsedeLinjer = this.filter.getOrDefault(malNavn, new HashSet<>());

        switch (filter.get("action").asText()) {
            case "add":
                kollapsedeLinjer.add(fold);
                break;
            case "remove":
                kollapsedeLinjer.remove(fold);
                break;
            default:
                return;
        }
        this.filter.putIfAbsent(malNavn, kollapsedeLinjer);
    }

    private Context insertTestData(JsonNode model, String malNavn) {
        return Context
                .newBuilder(model)
                .combine("malNavn", malNavn + "/" + malNavn)
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
                    .filter(p -> !p.toFile().isHidden())
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
            return konverterBrevTilPdf(malNavn, jsonService.getJsonFromString(payload));
        } catch (IOException e) {
            throw new RuntimeException("Kunne ikke lage pdf, malNavn={} " + malNavn, e);
        }
    }

    public String lagHtml(String malNavn, String payload) {
        try {
            return konverterBrevTilHtml(malNavn, jsonService.getJsonFromString(payload));
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

                Files.write(malPath, strippedHtmlSyntax.getBytes(StandardCharsets.UTF_8));
            } else {
                throw new IllegalArgumentException("Kan ikke hente markdown for payload=" + payload);
            }
        } catch (IOException e) {
            throw new RuntimeException("Feil ved lagring av mal=" + malNavn + " payload=" + payload, e);
        }
    }

    private String konverterBrevTilHtml(String malNavn, JsonNode payload) {
        Document styledHtml = konverterMalTilDokument(malNavn, payload, "html");
        return styledHtml.html();
    }

    private byte[] konverterBrevTilPdf(String malNavn, JsonNode payload) {
        Document styledHtml = konverterMalTilDokument(malNavn, payload, "pdf");
        dokumentGeneratorService.addDocumentParts(styledHtml);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        dokumentGeneratorService.genererPDF(styledHtml, outputStream);
        return outputStream.toByteArray();
    }

    private Document konverterMalTilDokument(String malNavn, JsonNode payload, String css) {
        try {
            JsonNode filter = Optional.ofNullable(payload.get("filter"))
                    .map(this::toObjectNode)
                    .orElse(null);

            JsonNode flettefelter = jsonService.extractInterleavingFields(
                    malNavn,
                    payload,
                    payload.get("useTestSet") != null && payload.get("useTestSet").asBoolean()
            );
            String compiledTemplate = hentKompilertMal(malNavn, flettefelter, filter);
            return dokumentGeneratorService.appendHtmlMetadata(compiledTemplate, css);
        } catch (IOException e) {
            throw new RuntimeException("Ukjent feil ved konvertering av brev mal=" + malNavn, e);
        }

    }

    private JsonNode toObjectNode(JsonNode textNode) {
        try {
            return new ObjectMapper().readTree(textNode.asText());
        } catch (IOException e) {
            return null;
        }
    }
}