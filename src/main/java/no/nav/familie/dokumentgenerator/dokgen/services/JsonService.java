package no.nav.familie.dokumentgenerator.dokgen.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.familie.dokumentgenerator.dokgen.feil.DokgenValideringException;
import no.nav.familie.dokumentgenerator.dokgen.util.MalUtil;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class JsonService {
    private static final Logger LOG = LoggerFactory.getLogger(JsonService.class);

    @Value("${path.content.root:./content/}")
    private Path contentRoot;

    private JsonNode readJsonFile(URI path) {
        if (path != null) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                return mapper.readTree(new File(path));
            } catch (IOException e) {
                LOG.error("Feil ved lesing av JSON", e);
            }
        }
        return null;
    }

    private JsonNode getTestSetField(String templateName, String testSet) {
        URI path = MalUtil.hentTestsett(contentRoot, templateName, testSet).toUri();
        return readJsonFile(path);
    }

    public JsonNode getJsonFromString(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(json);
    }

    public JsonNode extractInterleavingFields(String templateName, JsonNode jsonContent, boolean useTestSet) {
        JsonNode valueFields;
        if (useTestSet) {
            valueFields = getTestSetField(
                    templateName,
                    jsonContent.get("testSetName").textValue()
            );
        } else {
            return jsonContent.get("interleavingFields");
        }
        return valueFields;
    }

    public void validateTestData(Path jsonSchema,  String json) throws IOException {
        try (InputStream inputStream = new FileInputStream(jsonSchema.toFile())) {
            String stringSchema = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            JSONObject rawSchema = new JSONObject(new JSONTokener(stringSchema));
            Schema schema = SchemaLoader.load(rawSchema);

            schema.validate(new JSONObject(json)); // throws a ValidationException if this object is invalid
        } catch (ValidationException e) {
            throw new DokgenValideringException(e.toJSON().toString(), e);
        }
    }

    public String getEmptyTestData(String templateName) {
        try {
            return new String(Files.readAllBytes(MalUtil.hentTomtTestsett(contentRoot, templateName)));
        } catch (IOException e) {
            LOG.error("Kunne ikke lese tomt testdata", e);
        }
        return null;
    }
}
