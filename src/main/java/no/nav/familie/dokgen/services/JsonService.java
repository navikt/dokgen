package no.nav.familie.dokgen.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.familie.dokgen.feil.DokgenIkkeFunnetException;
import no.nav.familie.dokgen.feil.DokgenValideringException;
import no.nav.familie.dokgen.util.MalUtil;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class JsonService {

    private final Path contentRoot;

    @Autowired
    public JsonService(@Value("${path.content.root:./content/}") Path contentRoot) {
        this.contentRoot = contentRoot;
    }

    private JsonNode readJsonFile(Path path) {
        if (path != null) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                return mapper.readTree(path.toFile());
            } catch (FileNotFoundException e ) {
                throw new DokgenIkkeFunnetException("Kan ikke finne " + path.toString());
            } catch (IOException e) {
                throw new RuntimeException("Feil ved lesing av JSON");
            }
        }
        return null;
    }

    private JsonNode getTestSetField(String templateName, String testSet) {
        Path path = MalUtil.hentTestsett(contentRoot, templateName, testSet);
        return readJsonFile(path);
    }

    public JsonNode getJsonFromString(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(json);
    }

    public JsonNode extractInterleavingFields(String malNavn, JsonNode jsonContent, boolean useTestSet) {
        if (useTestSet) {
            JsonNode valueFields = getTestSetField(
                    malNavn,
                    jsonContent.get("testSetName").textValue()
            );
            if (valueFields == null) {
                throw new IllegalArgumentException("JSON node testSetName er påkrevd ved bruk av useTestSet=true");
            } else {
                return valueFields;
            }
        } else {
            JsonNode valueFields = jsonContent.get("interleavingFields");
            if (valueFields == null) {
                throw new IllegalArgumentException("JSON node interleavingFields er påkrevd ved bruk av useTestSet=false");
            } else {
                return valueFields;
            }
        }
    }

    public void validereJson(Path jsonSchema, String json) throws IOException {
        try (InputStream inputStream = new FileInputStream(jsonSchema.toFile())) {
            String stringSchema = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            JSONObject rawSchema = new JSONObject(new JSONTokener(stringSchema));
            Schema schema = SchemaLoader.load(rawSchema);

            schema.validate(new JSONObject(json)); // throws a ValidationException if this object is invalid
        } catch (ValidationException e) {
            Map<String, String> valideringsFeil = e.getCausingExceptions().stream()
                    .collect(Collectors.toMap(ValidationException::getPointerToViolation, ValidationException::getErrorMessage));
            throw new DokgenValideringException(valideringsFeil, e.toJSON().toString(), e);
        }
    }

    public String getEmptyTestData(String malNavn) {
        try {
            return new String(Files.readAllBytes(MalUtil.hentTomtTestsett(contentRoot, malNavn)));
        } catch (NoSuchFileException e) {
            throw new DokgenIkkeFunnetException("Fant ikke tomt testsett for mal " + malNavn);
        } catch (IOException e) {
            throw new RuntimeException("Kan ikke lese tomt testdata " + malNavn, e);
        }
    }
}
