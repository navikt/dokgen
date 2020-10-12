package no.nav.dokgen.services

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.dokgen.exceptions.DokgenNotFoundException
import no.nav.dokgen.exceptions.DokgenValidationException
import no.nav.dokgen.util.FileStructureUtil.getTemplateSchemaPath
import org.everit.json.schema.Schema
import org.everit.json.schema.ValidationException
import org.everit.json.schema.loader.SchemaLoader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.util.*
import java.util.stream.Collectors
import org.json.JSONObject
import org.json.JSONTokener

@Service
class JsonService @Autowired constructor(
    @param:Value("\${path.content.root:./content/}") private val contentRoot: Path
) {
    private fun readJsonFile(path: Path?): JsonNode? {
        return if (path != null) {
            Result.runCatching {
                val mapper = ObjectMapper()
                mapper.readTree(path.toFile())
            }.fold(
                onSuccess = { it },
                onFailure = { e ->
                    when (e) {
                        is FileNotFoundException -> throw DokgenNotFoundException("Kan ikke finne $path")
                        is IOException -> throw RuntimeException("Feil ved lesing av JSON")
                        else -> throw e
                    }
                }
            )
        } else null
    }

    @Throws(IOException::class)
    fun getJsonFromString(json: String?): JsonNode {
        val mapper = ObjectMapper()
        return mapper.readTree(json)
    }

    @Throws(IOException::class)
    fun getSchema(schemaPath: Path): Schema {
        val inputStream: InputStream = FileInputStream(schemaPath.toFile())
        val stringSchema = String(inputStream.readAllBytes(), StandardCharsets.UTF_8)
        val rawSchema = JSONObject(JSONTokener(stringSchema))
        return SchemaLoader.load(rawSchema)
    }

    @Throws(IOException::class)
    fun validereJson(schemaPath: Path, json: JsonNode?) {
        try {
            val schema: Schema = getSchema(schemaPath)
            schema.validate(JSONObject(Objects.requireNonNull(json, "Ved validering av flettefelt-json").toString()))
        } catch (e: ValidationException) {
            val valideringsFeil: Map<String, String> = e.getCausingExceptions().stream()
                .collect(
                    Collectors.toMap(
                        ValidationException::getPointerToViolation,
                        ValidationException::getErrorMessage
                    )
                )
            throw DokgenValidationException(valideringsFeil, e.toJSON().toString(), e)
        }
    }

    fun getSchemaAsString(malNavn: String): String {
        return try {
            val schemaPath = getTemplateSchemaPath(contentRoot, malNavn)
            val schema: Schema = getSchema(schemaPath)
            schema.toString()
        } catch (e: NoSuchFileException) {
            throw DokgenNotFoundException("Fant ikke tomt testsett for mal $malNavn")
        } catch (e: IOException) {
            throw RuntimeException("Kan ikke lese tomt testdata $malNavn", e)
        }
    }
}