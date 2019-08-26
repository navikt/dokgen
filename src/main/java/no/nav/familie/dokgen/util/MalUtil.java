package no.nav.familie.dokgen.util;

import java.nio.file.Path;

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
}
