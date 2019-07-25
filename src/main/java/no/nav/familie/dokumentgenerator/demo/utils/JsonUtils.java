package no.nav.familie.dokumentgenerator.demo.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;

@Service
public class JsonUtils {

    private JsonNode readJsonFile(URL path) {
        if (path != null) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                return mapper.readTree(new File(path.toURI()));
            } catch (IOException | URISyntaxException e) {
                System.out.println("Kan ikke finne JSON fil!");
                e.printStackTrace();
            }
        }
        return null;
    }

    private JsonNode getTestSetField(String templateName, String testSet){
        URL path = ClassLoader.getSystemResource("templates/" + templateName + "/testdata/" + testSet + ".json");
        return readJsonFile(path);
    }

    public JsonNode getJsonFromString(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(json);
    }

    public JsonNode extractInterleavingFields(String templateName, JsonNode jsonContent, boolean useTestSet){
        JsonNode valueFields;
        if(useTestSet){
            valueFields = getTestSetField(
                    templateName,
                    jsonContent.get("testSetName").textValue()
            );
        }
        else{
            return jsonContent.get("interleavingFields");
        }
        return valueFields;
    }

    public String validateTestData(String templateName, String json) {
        String statusMessage = null;
        String jsonSchemaLocation = "templates/" + templateName + "/testdata/" + templateName + ".schema.json";
        try (InputStream inputStream = ClassLoader.getSystemResourceAsStream(jsonSchemaLocation)) {

            if (inputStream == null) {
                return null;
            }

            JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
            Schema schema = SchemaLoader.load(rawSchema);

            try {
                schema.validate(new JSONObject(json)); // throws a ValidationException if this object is invalid
                statusMessage = "{ \"status\": \"Suksess!\" }";
            } catch (ValidationException e) {
                JSONObject jsonObject = e.toJSON();
                return jsonObject.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return statusMessage;
    }

}
