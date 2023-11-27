package no.nav.dokgen.services

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.JsonNodeValueResolver
import no.nav.dokgen.util.FileStructureUtil.getTemplateRootPath
import no.nav.dokgen.util.FileStructureUtil.getTemplatePath
import no.nav.dokgen.util.FileStructureUtil.getTemplateSchemaPath
import org.apache.commons.io.FileUtils
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.FileWriter
import java.io.IOException
import java.net.URISyntaxException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class TemplateServiceTests {
    private lateinit var malService: TemplateService
    private lateinit var templateRoot: Path

    @TempDir
    lateinit var temporaryFolder: Path

    @BeforeEach
    @Throws(IOException::class)
    fun setUp() {
        templateRoot = getTemplateRootPath(temporaryFolder)
        Files.createDirectories(templateRoot)
        val fileWriter = FileWriter(templateRoot.resolve("footer.hbs").toString())
        fileWriter.write(
            """
    
    $FOOTER_CONTENT
    """.trimIndent()
        )
        fileWriter.close()
        malService = TemplateService(temporaryFolder, DocumentGeneratorService(testContentPath!!), JsonService(temporaryFolder))
    }

    @Test
    fun skalHentAlleMalerReturnererTomtSett() {
        Assertions.assertThat(malService.listTemplates()).isEmpty()
    }

    @Test
    @Throws(IOException::class)
    fun skalHentAlleMaler() {
        Files.createDirectory(templateRoot.resolve("MAL1"))
        Assertions.assertThat(malService.listTemplates()).containsExactly("MAL1")
    }

    @Test
    fun skalReturnereExceptionVedHentingAvMal() {
        assertThatThrownBy { malService.getTemplate("IkkeEksisterendeMal") }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessage("Kan ikke finne mal med navn IkkeEksisterendeMal")
    }

    @Test
    fun skalReturnereMal() {
        val malPath = templateRoot.resolve("MAL")
        Files.createDirectories(malPath)
        val templatePath = getTemplatePath(temporaryFolder, "MAL")
        Files.write(templatePath, "maldata".toByteArray())


        // expect
        val (_, content) = malService.getTemplate("MAL")

        // when
        Assertions.assertThat(content).isEqualTo("maldata")
    }

    @Test
    fun skalReturnereMalDerMalnavnErEnPath() {
        val malpathString = "EN/MAL/SOM/LIGGER/HER"
        val malPath = templateRoot.resolve(malpathString)
        Files.createDirectories(malPath)
        val templatePath = getTemplatePath(temporaryFolder, malpathString)
        Files.write(templatePath, "maldata".toByteArray())


        // expect
        val (_, content) = malService.getTemplate(malpathString)

        // when
        Assertions.assertThat(content).isEqualTo("maldata")
    }

    @Test
    fun skalReturnereIllegalArgumentExceptionVedLagreMalUtenPayloadUtenMarkdown() {
        assertThatThrownBy { malService.saveTemplate("tomPayload", "{}") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Kan ikke hente markdown for payload={}")
    }

    @Test
    @Throws(IOException::class)
    fun skalLagreMal() {

        // expect
        malService.saveTemplate(MALNAVN, GYLDIG_PAYLOAD)
        val malData = Files.readString(getTemplatePath(temporaryFolder, MALNAVN))
        Assertions.assertThat(malData).isEqualTo(MARKDOWN)
    }

    @Test
    @Throws(IOException::class)
    fun skal_compile_inline_with_a_footer() {
        val tmp = malService.compileInLineTemplate("this would be a header\n{{> footer }}")
        val node: JsonNode = JsonNodeFactory.instance.arrayNode()
        val compiledStr = tmp!!.apply(node)
        Assertions.assertThat(compiledStr).endsWith(FOOTER_CONTENT)
    }

    @Test
    @Throws(IOException::class)
    fun skal_overskrive_alt_ved_lagring_av_nytt_slankere_innhold() {
        skalLagreMal()
        malService.saveTemplate(MALNAVN, lagTestPayload("\"" + MARKDOWN.substring(3) + "\"", MERGE_FIELDS))
        val malData = Files.readString(getTemplatePath(temporaryFolder, MALNAVN))
        Assertions.assertThat(malData).isEqualTo(MARKDOWN.substring(3))
    }

    @Test
    @Throws(IOException::class)
    fun skalHenteHtml() {
        val malNavn = "html"
        FileUtils.writeStringToFile(
            getTemplateSchemaPath(temporaryFolder, malNavn).toFile(),
            TOM_JSON,
            StandardCharsets.UTF_8
        )

        // expect
        malService.saveTemplate(malNavn, GYLDIG_PAYLOAD)
        val html = malService.createHtml(malNavn, "{\"name\":\"Peter\"}")
        Assertions.assertThat(html).contains("<h1>Hei, Peter</h1>")
    }

    @Test
    @Throws(IOException::class)
    fun skalHentePdf() {
        val malNavn = "pdf"
        FileUtils.writeStringToFile(
            getTemplateSchemaPath(temporaryFolder, malNavn).toFile(),
            TOM_JSON,
            StandardCharsets.UTF_8
        )

        // expect
        malService.saveTemplate(malNavn, GYLDIG_PAYLOAD)
        val pdf = malService.createPdf(malNavn, GYLDIG_PAYLOAD)
        Assertions.assertThat(pdf[1]).isEqualTo(0x50.toByte()) //P
        Assertions.assertThat(pdf[2]).isEqualTo(0x44.toByte()) //D
        Assertions.assertThat(pdf[3]).isEqualTo(0x46.toByte()) //F
    }

    @Test
    @Throws(IOException::class)
    fun skalHentePdfFraVariation() {
        val malNavn = "pdf"
        val templateVariation = "template_NN"
        FileUtils.writeStringToFile(
            getTemplateSchemaPath(temporaryFolder, malNavn).toFile(),
            TOM_JSON,
            StandardCharsets.UTF_8
        )

        // expect
        malService.saveTemplate(malNavn, GYLDIG_PAYLOAD, templateVariation)
        val pdf = malService.createPdf(malNavn, GYLDIG_PAYLOAD, templateVariation)
        Assertions.assertThat(pdf[1]).isEqualTo(0x50.toByte()) //P
        Assertions.assertThat(pdf[2]).isEqualTo(0x44.toByte()) //D
        Assertions.assertThat(pdf[3]).isEqualTo(0x46.toByte()) //F
    }

    @Test
    fun skalCompileNorwegianDatetimeHelper() {
        val jsonNode: JsonNode = JsonNodeFactory.instance.objectNode().apply { put("timestamp", "2023-08-10T15:54:35") }
        val context = Context.newBuilder(jsonNode).resolver(JsonNodeValueResolver.INSTANCE).build()
        val tmpl = malService.compileInLineTemplate("{{norwegian-datetime timestamp}}")
        val compiledStr = tmpl!!.apply(context)
        Assertions.assertThat(compiledStr).isEqualTo("10.08.2023 15:54")
    }

    companion object {
        private const val MALNAVN = "lagretMal"
        private const val MARKDOWN = "# Hei, {{name}}"
        private const val MARKDOWN_CONTENT = "\"" + MARKDOWN + "\""
        private const val MERGE_FIELDS = "{\"name\": \"Peter\"}"
        private val GYLDIG_PAYLOAD = lagTestPayload(MARKDOWN_CONTENT, MERGE_FIELDS)
        private const val FOOTER_CONTENT = "this would be the footer."
        private fun lagTestPayload(markdownContent: String, interleavingFields: String): String {
            return "{\"markdownContent\": " + markdownContent +
                    ", \"interleavingFields\": " + interleavingFields +
                    ", \"useTestSet\": false}"
        }

        val testContentPath: Path?
            get() = try {
                Paths.get(
                    DocumentGeneratorServiceTests::class.java.protectionDomain.codeSource.location.toURI()
                ).resolve(
                    Paths.get("test-content")
                ).toAbsolutePath()
            } catch (e: URISyntaxException) {
                null
            }
        private const val TOM_JSON = "{}"
    }
}