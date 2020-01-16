package no.nav.dokgen.services;

import no.nav.dokgen.exceptions.DokgenNotFoundException;
import no.nav.dokgen.util.FileStructureUtil;
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
public class TestDataService {

    private final Path contentRoot;
    private final JsonService jsonService;

    @Autowired
    TestDataService(@Value("${path.content.root:./content/}") Path contentRoot, JsonService jsonService) {
        this.contentRoot = contentRoot;
        this.jsonService = jsonService;
    }

    public List<String> listTestData(String templateName) {
        try (Stream<Path> paths = Files.list(FileStructureUtil.getTestDataRootPath(contentRoot, templateName))) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(x -> FilenameUtils.getBaseName(x.getFileName().toString()))
                    .collect(Collectors.toList());
        } catch (NoSuchFileException e) {
            throw new DokgenNotFoundException("Kan ikke hente testdatasett for ukjent mal=" + templateName);
        } catch (IOException e) {
            throw new RuntimeException("Kan ikke hente testdatasett for mal=" + templateName, e);
        }
    }

    public String getTestData(String templateName, String testDataName) {
        Path testSetPath = FileStructureUtil.getTestDataPath(contentRoot, templateName, testDataName);
        try {
            byte[] content = Files.readAllBytes(testSetPath);
            return new String(content);
        } catch (NoSuchFileException e) {
            throw new DokgenNotFoundException("Kan ikke hente testdatasett for ukjent mal=" + templateName);
        } catch (IOException e) {
            throw new RuntimeException("Kan ikke hente testdatasett for mal=" + templateName, e);
        }
    }

    public String saveTestData(String templateName, String testDataName, String testData) {
        JSONObject testDataObject = new JSONObject(testData);
        String testDataContent = testDataObject.toString(2);
        try {
            jsonService.validereJson(FileStructureUtil.getTemplateSchemaPath(contentRoot, templateName), jsonService.getJsonFromString(testData));
            Files.write(FileStructureUtil.getTestDataPath(contentRoot, templateName, testDataName), testDataContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new RuntimeException("Feil ved lagring av testdata for mal=" + templateName, e);
        }
        return testDataName;
    }
}
