package no.nav.dokgen.services;

import static no.nav.dokgen.util.DocFormat.HTML;
import static no.nav.dokgen.util.DocFormat.PDF;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.everit.json.schema.ValidationException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

import no.nav.dokgen.controller.api.CreateDocumentRequest;
import no.nav.dokgen.exceptions.DokgenNotFoundException;
import no.nav.dokgen.resources.TemplateResource;
import no.nav.dokgen.util.DocFormat;
import no.nav.dokgen.util.FileStructureUtil;


@Service
public class TemplateService {
    private static final Logger LOG = LoggerFactory.getLogger(TemplateService.class);

    private final Handlebars handlebars;
    private final DocumentGeneratorService documentGeneratorService;
    private final JsonService jsonService;

    private final Path contentRoot;

    @Autowired
    TemplateService(@Value("${path.content.root:./content/}") Path contentRoot,
                    DocumentGeneratorService documentGeneratorService,
                    JsonService jsonService) {

        this.contentRoot = contentRoot;

        handlebars = Files.exists(FileStructureUtil.getTemplateRootPath(contentRoot))
                ? new Handlebars(new FileTemplateLoader(FileStructureUtil.getTemplateRootPath(contentRoot).toFile()))
                : new Handlebars();
        handlebars.registerHelper("eq", ConditionalHelpers.eq);
        handlebars.registerHelper("neq", ConditionalHelpers.neq);
        handlebars.registerHelpers(StringHelpers.class);

        this.documentGeneratorService = documentGeneratorService;
        this.jsonService = jsonService;
    }

    public Template compileInLineTemplate(String templateContent) {
        try {
            return handlebars.compileInline(templateContent);
        } catch (IOException e) {
            LOG.error("Kompilering av templates feiler", e);
        }
        return null;
    }

    public String compileInlineAndApply(String templateContent, Context data) {
        try {
            return handlebars.compileInline(templateContent).apply(data);
        } catch (IOException e) {
            LOG.warn("Kompilering av malinnhold feilet: " + templateContent);
        }
        return templateContent;
    }

    private String getCompiledTemplate(TemplateResource templateResource, JsonNode mergeFields) throws ValidationException, IOException {
        Template template = compileInLineTemplate(templateResource.getContent());
        jsonService.validereJson(FileStructureUtil.getTemplateSchemaPath(contentRoot, templateResource.name), mergeFields);
        if (template != null) {
            return template.apply(with(mergeFields));
        }
        return null;
    }

    private Context with(JsonNode model) {
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
        return getTemplate(templateName, FileStructureUtil.getTemplatePath(contentRoot, templateName));
    }

    public TemplateResource getTemplate(String templateName, String variation) {
        return getTemplate(templateName, FileStructureUtil.getTemplatePath(contentRoot, templateName, variation));
    }

    private TemplateResource getTemplate(String templateName, Path templatePath) {
        try {
            TemplateResource resource = new TemplateResource(templateName);
            String content = Files.readString(templatePath, StandardCharsets.UTF_8);
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

    public byte[] createPdf(String templateName, String payload, String variation) {
        TemplateResource templateResource = getTemplate(templateName, variation);
        return createPdf(templateResource, payload);
    }

    public String createHtml(String templateName, String payload) {
        TemplateResource templateResource = getTemplate(templateName);
        return createHtml(templateResource, payload);
    }

    public String createHtml(String templateName, String payload, String variation) {
        TemplateResource templateResource = getTemplate(templateName, variation);
        return createHtml(templateResource, payload);
    }

    public String createMarkdown(String templateName, String mergefields) {
        TemplateResource templateResource = getTemplate(templateName);
        return createMarkdown(templateName, mergefields, templateResource);
    }

    public String createMarkdown(String templateName, String mergefields, String variation) {
        TemplateResource templateResource = getTemplate(templateName, variation);
        return createMarkdown(templateName, mergefields, templateResource);
    }

    private String createMarkdown(String templateName, String mergefields, TemplateResource templateResource) {
        try {
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

    public Document createDocument(CreateDocumentRequest request, String templateName) {
        try {
            var template = getCompiledTemplate(request, templateName);
            JsonNode headerFields = request.getHeaderFields() == null || !request.isIncludeHeader()
                    ? null
                    : jsonService.getJsonFromString(request.getHeaderFields());
            return convertToDocument(request.getDocFormat(), template, request.isIncludeHeader(), headerFields);
        } catch (IOException e) {
            throw new RuntimeException("Kunne ikke mappe header-felter til Json", e);
        }
    }

    private TemplateResource getCompiledTemplate(CreateDocumentRequest request, String templateName) {
        try {
            var template = new TemplateResource(templateName);
            if (request.getTemplateContent() != null) {
                // use precompiled or custom template content
                if (request.isPrecompiled()) {
                    template.compiledContent = request.getTemplateContent();
                } else {
                    JsonNode mergeFields = jsonService.getJsonFromString(request.getMergeFields());
                    template.compiledContent = compileInlineAndApply(request.getTemplateContent(), with(mergeFields));
                }
            } else {
                // load and compile template from file.
                JsonNode mergeFields = jsonService.getJsonFromString(request.getMergeFields());
                template.compiledContent = getCompiledTemplate(getTemplate(templateName), mergeFields);
            }
            return template;
        } catch (IOException e) {
            throw new RuntimeException("Kunne ikke lage dokument med angitt flettefelt-json", e);
        }
    }

    public void saveTemplate(String templateName, String payload) {
        Path malPath = FileStructureUtil.getTemplatePath(contentRoot, templateName);
        saveTemplate(templateName, payload, malPath);
    }

    public void saveTemplate(String templateName, String payload, String variation) {
        Path malPath = FileStructureUtil.getTemplatePath(contentRoot, templateName, variation);
        saveTemplate(templateName, payload, malPath);
    }

    private void saveTemplate(String templateName, String payload, Path malPath) {
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

                Files.createDirectories(malPath.getParent());

                Files.write(malPath, strippedHtmlSyntax.getBytes(StandardCharsets.UTF_8));
            } else {
                throw new IllegalArgumentException("Kan ikke hente markdown for payload=" + payload);
            }
        } catch (IOException e) {
            throw new RuntimeException("Feil ved lagring av mal=" + templateName + " payload=" + payload, e);
        }
    }

    private String convertToHtml(TemplateResource template, JsonNode mergeFields) throws IOException {
        template.compiledContent = getCompiledTemplate(template, mergeFields);
        Document styledHtml = convertToDocument(HTML, template, false, null);
        return styledHtml.html();
    }

    private byte[] convertToPdf(TemplateResource template, JsonNode mergeFields) throws IOException {
        template.compiledContent = getCompiledTemplate(template, mergeFields);
        Document styledHtml = convertToDocument(PDF, template, true, mergeFields);
        return generatePdf(styledHtml);
    }

    public byte[] generatePdf(Document document) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        documentGeneratorService.genererPDF(document, outputStream);
        return outputStream.toByteArray();
    }

    private Document convertToDocument(DocFormat format, TemplateResource template, boolean wrapDocument, JsonNode headerFields) {
        Document styledHtml = documentGeneratorService.appendHtmlMetadata(template.compiledContent, format);
        if (wrapDocument) {
            documentGeneratorService.wrapDocument(styledHtml, format, headerFunction(format, template.name, headerFields));
        }
        return styledHtml;
    }

    private Function<String, String> headerFunction(DocFormat format, String templateName, JsonNode mergeFields) {
        return header -> {
            validateIfRequired(mergeFields, format);
            return compileInlineAndApply(header, with(mergeFields).combine("templateName", templateName));
        };
    }

    private void validateIfRequired(JsonNode mergeFields, DocFormat format) {
        try {
            jsonService.validereJson(FileStructureUtil.getFormatSchema(contentRoot, format), mergeFields);
        } catch (FileNotFoundException ignore) {
            // This header does not require validation
        } catch (Exception e) {
            throw new RuntimeException("Feil ved validering av header-felter", e);
        }
    }

}