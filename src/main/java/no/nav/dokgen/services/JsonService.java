package no.nav.dokgen.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.dokgen.exceptions.DokgenNotFoundException;
import no.nav.dokgen.exceptions.DokgenValidationException;
import no.nav.dokgen.util.FileStructureUtil;
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
            } catch (FileNotFoundException e) {
                throw new DokgenNotFoundException("Kan ikke finne " + path.toString());
            } catch (IOException e) {
                throw new RuntimeException("Feil ved lesing av JSON");
            }
        }
        return null;
    }


    public JsonNode getJsonFromString(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(json);
    }

    public Schema getSchema(Path schemaPath) throws IOException {
        InputStream inputStream = new FileInputStream(schemaPath.toFile());
        String stringSchema = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        JSONObject rawSchema = new JSONObject(new JSONTokener(stringSchema));
        return SchemaLoader.load(rawSchema);
    }

    public void validereJson(Path schemaPath, String json) throws IOException {
        try {
            Schema schema = getSchema(schemaPath);
            schema.validate(new JSONObject(json));
        } catch (ValidationException e) {
            Map<String, String> valideringsFeil = e.getCausingExceptions().stream()
                    .collect(Collectors.toMap(ValidationException::getPointerToViolation, ValidationException::getErrorMessage));
            throw new DokgenValidationException(valideringsFeil, e.toJSON().toString(), e);
        }
    }

    public String getSchemaAsString(String malNavn) {
        try {
            Path schemaPath = FileStructureUtil.getTemplateSchemaPath(contentRoot, malNavn);
            Schema schema = getSchema(schemaPath);
            return  schema.toString();
        } catch (NoSuchFileException e) {
            throw new DokgenNotFoundException("Fant ikke tomt testsett for mal " + malNavn);
        } catch (IOException e) {
            throw new RuntimeException("Kan ikke lese tomt testdata " + malNavn, e);
        }
    }
}
