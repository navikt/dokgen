package no.nav.dokgen.services;

import static no.nav.dokgen.util.FileStructureUtil.Fold;

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
import com.github.jknack.handlebars.helper.StringHelpers;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import no.nav.dokgen.exceptions.DokgenNotFoundException;
import no.nav.dokgen.resources.TemplateResource;
import no.nav.dokgen.util.DocFormat;
import no.nav.dokgen.util.FileStructureUtil;
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
    private final DocumentGeneratorService documentGeneratorService;
    private final JsonService jsonService;

    private final Path contentRoot;

    private Map<String, Set<Fold>> filter = new HashMap<>();

    @Autowired
    TemplateService(@Value("${path.content.root:./content/}") Path contentRoot, DocumentGeneratorService documentGeneratorService, JsonService jsonService) {
        this.contentRoot = contentRoot;

        handlebars = Files.exists(FileStructureUtil.getTemplateRootPath(contentRoot)) ? new Handlebars(new FileTemplateLoader(FileStructureUtil.getTemplateRootPath(contentRoot).toFile())) : new Handlebars();
        handlebars.registerHelper("eq", ConditionalHelpers.eq);
        handlebars.registerHelper("neq", ConditionalHelpers.neq);
        handlebars.registerHelper("dateFormat", StringHelpers.dateFormat);

        this.documentGeneratorService = documentGeneratorService;
        this.jsonService = jsonService;
    }

    private Template compileTemplate(String templateName) {
        try {
            return handlebars.compile(templateName + "/template");
        } catch (IOException e) {
            LOG.error("Kompilering av templates feiler", e);
        }
        return null;
    }

    public Template compileInLineTemplate(String templateContent) {
        try {
            return handlebars.compileInline(templateContent);
        } catch (IOException e) {
            LOG.error("Kompilering av templates feiler", e);
        }
        return null;
    }

    private String getCompiledTemplate(TemplateResource templateResource, JsonNode mergeFields) throws ValidationException, IOException {
        Template template = compileInLineTemplate(templateResource.getContent());
        jsonService.validereJson(FileStructureUtil.getTemplateSchemaPath(contentRoot, templateResource.name), mergeFields.toString());
        if (template != null) {
            return template.apply(insertTestData(mergeFields, templateResource.name));
        }
        return null;
    }

    private String applyFilter(Set<Fold> filter, Stream<String> content) {
        return content.filter(excludedLines(filter))
                .collect(Collectors.joining("\n"));
    }

    private Predicate<String> excludedLines(Set<Fold> filter) {
        var lineNumber = new AtomicInteger(0);
        return line -> {
            boolean include = filter.stream().noneMatch(fold -> fold.contains(lineNumber.get()));
            lineNumber.incrementAndGet();
            return include;
        };
    }

    public Context insertTestData(JsonNode model, String templateName) {
        return Context
                .newBuilder(model)
                .resolver(JsonNodeValueResolver.INSTANCE,
                        JavaBeanValueResolver.INSTANCE,
                        FieldValueResolver.INSTANCE,
                        MapValueResolver.INSTANCE,
                        MethodValueResolver.INSTANCE
                ).build();
    }

    public List<String> listTemplates() {
        try (Stream<Path> paths = Files.list(FileStructureUtil.getTemplateRootPath(contentRoot))) {
            return paths
                    .filter(Files::isDirectory)
                    .filter(p -> !p.toFile().isHidden())
                    .map(x -> x.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public TemplateResource getTemplate(String templateName) {
        try {
            TemplateResource resource = new TemplateResource(templateName);
            String content = Files.readString(FileStructureUtil.getTemplatePath(contentRoot, templateName), StandardCharsets.UTF_8);
            resource.setContent(content);
            return resource;
        } catch (NoSuchFileException e) {
            throw new DokgenNotFoundException("Kan ikke finne mal med navn " + templateName);
        } catch (IOException e) {
            throw new RuntimeException("Kan ikke hente mal " + templateName, e);
        }
    }

    public byte[] createPdf(String templateName, String payload) {
        TemplateResource templateResource = getTemplate(templateName);
        return createPdf(templateResource, payload);
    }

    public String createHtml(String templateName, String payload) {
        TemplateResource templateResource = getTemplate(templateName);
        return createHtml(templateResource, payload);
    }

    public String createMarkdown(String templateName, String mergefields) {
        try {
            TemplateResource templateResource = getTemplate(templateName);
            return getCompiledTemplate(templateResource, jsonService.getJsonFromString(mergefields));
        } catch (IOException e) {
            throw new RuntimeException("Kunne ikke lage Markdown, templateName={} " + templateName, e);
        }
    }

    public byte[] createPdf(TemplateResource templateResource, String payload) {
        try {
            return convertToPdf(templateResource, jsonService.getJsonFromString(payload));
        } catch (IOException e) {
            throw new RuntimeException("Kunne ikke lage pdf, templateName={} " + templateResource.name, e);
        }
    }

    public String createHtml(TemplateResource templateResource, String payload) {
        try {
            return convertToHtml(templateResource, jsonService.getJsonFromString(payload));
        } catch (IOException e) {
            throw new RuntimeException("Kunne ikke lage HTML, templateName={} " + templateResource.name, e);
        }
    }

    public void saveTemplate(String templateName, String payload) {
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

                Path malPath = FileStructureUtil.getTemplatePath(contentRoot, templateName);
                Files.createDirectories(malPath.getParent());

                Files.write(malPath, strippedHtmlSyntax.getBytes(StandardCharsets.UTF_8));
            } else {
                throw new IllegalArgumentException("Kan ikke hente markdown for payload=" + payload);
            }
        } catch (IOException e) {
            throw new RuntimeException("Feil ved lagring av mal=" + templateName + " payload=" + payload, e);
        }
    }

    private String convertToHtml(TemplateResource template, JsonNode mergeFields) {
        Document styledHtml = convertToDocument(template, mergeFields, DocFormat.HTML);
        return styledHtml.html();
    }

    private byte[] convertToPdf(TemplateResource template, JsonNode mergeFields) {
        Document styledHtml = convertToDocument(template, mergeFields, DocFormat.PDF);
        documentGeneratorService.wrapDocument(styledHtml, DocFormat.PDF);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        documentGeneratorService.genererPDF(styledHtml, outputStream);
        return outputStream.toByteArray();
    }

    private Document convertToDocument(TemplateResource template, JsonNode mergeFields, DocFormat format) {
        try {
            String markdownDocument = getCompiledTemplate(template, mergeFields);
            return documentGeneratorService.appendHtmlMetadata(markdownDocument, format);
        } catch (IOException e) {
            throw new RuntimeException("Ukjent feil ved konvertering av brev", e);
        }

    }

}