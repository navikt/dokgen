package no.nav.dokgen.util;

import java.nio.file.Path;

public class FileStructureUtil {

    private static final String DEFAULT_VARIATION = "template";

    public static Path getTemplateSchemaPath(Path contentRoot, String templateName) {
        return contentRoot.resolve("templates/" + templateName + "/schema.json");
    }

    public static Path getTemplatePath(Path contentRoot, String templateName) {
        return getTemplatePath(contentRoot, templateName, DEFAULT_VARIATION);
    }

    public static Path getTemplatePath(Path contentRoot, String templateName, String variation) {
        return contentRoot.resolve("templates/" + templateName + "/" + variation + ".hbs");
    }

    public static Path getTestDataPath(Path contentRoot, String templateName, String testDataName) {
        return contentRoot.resolve("templates/" + templateName + "/testdata/" + testDataName + ".json");
    }

    public static Path getTestDataRootPath(Path contentRoot, String templateName) {
        return contentRoot.resolve(String.format("templates/%s/testdata/", templateName));
    }

    public static Path getTemplateRootPath(Path contentRoot) {
        return contentRoot.resolve("templates");
    }

    public static Path getFormatSchema(Path contentRoot, DocFormat format) {
        return contentRoot.resolve("formats/" + format + "schema.json");
    }

    public static Path getFormatHeader(Path contentRoot, DocFormat format) {
        return contentRoot.resolve("formats/" + format + "/header.html");
    }

    public static Path getFormatFooter(Path contentRoot, DocFormat format) {
        return contentRoot.resolve("formats/" + format + "/footer.html");
    }

    public static Path getCss(Path contentRoot, DocFormat format) {
        return contentRoot.resolve("formats/" + format + "/style.css");
    }

    public static Path getFont(Path contentRoot,String fontName) {
        return contentRoot.resolve("fonts/" + fontName);
    }

}
