package no.nav.familie.dokgen.services;

import no.nav.familie.dokgen.feil.DokgenIkkeFunnetException;
import no.nav.familie.dokgen.util.MalUtil;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TestdataService {

    private final Path contentRoot;
    private final JsonService jsonService;

    @Autowired
    TestdataService(@Value("${path.content.root:./content/}") Path contentRoot, JsonService jsonService) {
        this.contentRoot = contentRoot;
        this.jsonService = jsonService;
    }

    public List<String> hentTestdatasettForMal(String malNavn) {
        try (Stream<Path> paths = Files.list(MalUtil.hentTestdataFolder(contentRoot, malNavn))) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(x -> FilenameUtils.getBaseName(x.getFileName().toString()))
                    .collect(Collectors.toList());
        } catch (NoSuchFileException e) {
            throw new DokgenIkkeFunnetException("Kan ikke hente testdatasett for ukjent mal=" + malNavn);
        } catch (IOException e) {
            throw new RuntimeException("Kan ikke hente testdatasett for mal=" + malNavn, e);
        }
    }

    public String hentTomtTestsett(String templateName) {
        return jsonService.getEmptyTestData(templateName);
    }

    public String lagTestsett(String malNavn, String payload) {

        JSONObject obj = new JSONObject(payload);
        String testSetName = obj.getString("name");
        if (testSetName == null) {
            throw new IllegalArgumentException(("Payload mangler property name for lagring av testsett"));
        }
        String testSetContent = obj.getJSONObject("content").toString(4);

        try {
            jsonService.validereJson(MalUtil.hentJsonSchemaForMal(contentRoot, malNavn), testSetContent);
            Files.write(MalUtil.hentTestsett(contentRoot, malNavn, testSetName), testSetContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new RuntimeException("Feil ved lagring av testdata for mal=" + malNavn, e);
        }

        return testSetName;
    }
}
