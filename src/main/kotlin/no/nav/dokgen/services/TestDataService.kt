package no.nav.dokgen.services

import no.nav.dokgen.exceptions.DokgenNotFoundException
import no.nav.dokgen.util.FileStructureUtil.getTemplateSchemaPath
import no.nav.dokgen.util.FileStructureUtil.getTestDataPath
import no.nav.dokgen.util.FileStructureUtil.getTestDataRootPath
import org.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.stream.Collectors

@Service
class TestDataService @Autowired internal constructor(
    @Value("\${path.content.root:./content/}") private val contentRoot: Path,
    private val jsonService: JsonService
) {
    fun listTestData(templateName: String): List<String> {
        return Result.runCatching {
            Files.list(getTestDataRootPath(contentRoot, templateName)).use { paths ->
                paths
                    .filter(Files::isRegularFile)
                    .map { path -> path.fileName.toString() }
                    .map { filename -> filename.substringBeforeLast(".") }
                    .collect(Collectors.toList())
            }
        }.fold(
            onSuccess = { it },
            onFailure = { e ->
                when (e) {
                    is NoSuchFileException -> throw DokgenNotFoundException("Kan ikke hente testdatasett for ukjent mal=$templateName")
                    is IOException -> throw RuntimeException("Kan ikke hente testdatasett for mal=$templateName", e)
                    else -> throw e
                }
            }
        )
    }

    fun getTestData(templateName: String, testDataName: String): String {
        val testSetPath = getTestDataPath(contentRoot, templateName, testDataName)
        return try {
            val content = Files.readAllBytes(testSetPath)
            String(content)
        } catch (e: NoSuchFileException) {
            throw DokgenNotFoundException("Kan ikke hente testdatasett for ukjent mal=$templateName")
        } catch (e: IOException) {
            throw RuntimeException("Kan ikke hente testdatasett for mal=$templateName", e)
        }
    }

    fun saveTestData(templateName: String, testDataName: String, testData: String): String {
        val testDataObject = JSONObject(testData)
        val testDataContent = testDataObject.toString(2)
        try {
            jsonService.validereJson(
                getTemplateSchemaPath(contentRoot, templateName),
                jsonService.getJsonFromString(testData)
            )
            Files.write(
                getTestDataPath(contentRoot, templateName, testDataName), testDataContent.toByteArray(
                    StandardCharsets.UTF_8
                ), StandardOpenOption.CREATE
            )
        } catch (e: IOException) {
            throw RuntimeException("Feil ved lagring av testdata for mal=$templateName", e)
        }
        return testDataName
    }
}