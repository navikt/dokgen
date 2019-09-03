package no.nav.familie.dokgen.util;

import java.nio.file.Path;
import java.util.Objects;

public class MalUtil {

    public static Path hentJsonSchemaForMal(Path contentRoot, String malNavn) {
        return contentRoot.resolve("templates/" + malNavn + "/" + malNavn + ".schema.json");
    }

    public static Path hentMal(Path contentRoot, String malNavn) {
        return contentRoot.resolve("templates/" + malNavn + "/" + malNavn + ".hbs");
    }

    public static Path hentTomtTestsett(Path contentRoot, String malNavn) {
        return contentRoot.resolve("templates/" + malNavn + "/TomtTestsett.json");
    }

    public static Path hentTestsett(Path contentRoot, String malNavn, String testsett) {
        return contentRoot.resolve("templates/" + malNavn + "/testdata/" + testsett + ".json");
    }

    public static Path hentTestdataFolder(Path contentRoot, String malNavn) {
        return contentRoot.resolve(String.format("templates/%s/testdata/", malNavn));
    }

    public static Path hentMalRoot(Path contentRoot) {
        return contentRoot.resolve("templates");
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
