package no.nav.dokgen.util;

import java.nio.file.Path;
import java.util.Objects;

public class FileStructureUtil {

    public static Path getTemplateSchemaPath(Path contentRoot, String templateName) {
        return contentRoot.resolve("templates/" + templateName + "/schema.json");
    }

    public static Path getTemplatePath(Path contentRoot, String templateName) {
        return contentRoot.resolve("templates/" + templateName + "/template.hbs");
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

    public static final class Fold {
        int start;
        int end;

        public Fold(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public boolean contains(int lineNr) {
            return lineNr >= start && lineNr <= end;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Fold fold = (Fold) o;
            return start == fold.start &&
                    end == fold.end;
        }

        @Override
        public int hashCode() {
            return Objects.hash(start, end);
        }
    }
}
