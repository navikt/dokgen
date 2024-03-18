package no.nav.dokgen.services

import com.openhtmltopdf.extend.FSSupplier
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import com.openhtmltopdf.svgsupport.BatikSVGDrawer
import com.openhtmltopdf.util.XRLog
import no.nav.dokgen.util.DocFormat
import no.nav.dokgen.util.FileStructureUtil.getCss
import no.nav.dokgen.util.FileStructureUtil.getFormatFooter
import no.nav.dokgen.util.FileStructureUtil.getFormatHeader
import org.commonmark.Extension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.node.Node
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.jsoup.Jsoup
import org.jsoup.helper.W3CDom
import org.jsoup.nodes.Document
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import org.springframework.util.FileCopyUtils
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.function.Function


@Service
class DocumentGeneratorService @Autowired constructor(
    @Value("\${path.content.root:./content/}") private val contentRoot: Path
) {
    fun wrapDocument(document: Document, format: DocFormat, headerFunction: Function<String?, String?>) {
        try {
            val header = Files.readString(getFormatHeader(contentRoot, format), UTF_8)
            val footer = Files.readString(getFormatFooter(contentRoot, format), UTF_8)
            val body = document.body()
            headerFunction.apply(header)?.let { body.prepend(it) }
            body.append(footer)
        } catch (e: IOException) {
            throw RuntimeException("Kunne ikke legge til header/footer på dokumentet", e)
        }
    }

    fun appendHtmlMetadata(markdown: String, format: DocFormat): Document {
        val convertedTemplate = convertMarkdownTemplateToHtml(markdown)
        val document = Jsoup.parse("<div id=\"content\">$convertedTemplate</div>")
        val head = document.head()
        head.append("<meta charset=\"UTF-8\">")
        head.append("<style>" + hentCss(format) + "</style>")
        return document
    }

    fun fontSuppliers(builder: PdfRendererBuilder) {
        val sourceSansProFamily = "Source Sans Pro"
        builder.useFont(
            TTFontSupplier("SourceSansPro-Regular.ttf"),
            sourceSansProFamily,
            400,
            BaseRendererBuilder.FontStyle.NORMAL,
            true
        )
        builder.useFont(
            TTFontSupplier("SourceSansPro-Bold.ttf"),
            sourceSansProFamily,
            700,
            BaseRendererBuilder.FontStyle.OBLIQUE,
            true
        )
        builder.useFont(
            TTFontSupplier("SourceSansPro-It.ttf"),
            sourceSansProFamily,
            400,
            BaseRendererBuilder.FontStyle.ITALIC,
            true
        )
    }

    fun genererPDF(html: Document, outputStream: ByteArrayOutputStream?) {
        try {
            val doc = W3CDom().fromJsoup(html)
            outputStream?.let { os ->
                PdfRendererBuilder()
                    .apply { fontSuppliers(this) }
                    .withW3cDocument(doc, "")
                    .useSVGDrawer(BatikSVGDrawer())
                    .useColorProfile(colorProfile)
                    .usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_2_U)
                    .usePdfUaAccessbility(true)
                    .toStream(os)
                    .run()
            }
        } catch (e: Exception) {
            throw RuntimeException("Feil ved generering av pdf", e)
        }
    }

    private fun convertMarkdownTemplateToHtml(content: String): String {
        val document = parseDocument(content)
        return renderToHTML(document)
    }


    private fun parseDocument(content: String): Node {
        return markdownToHtmlParser.parse(content)
    }

    private fun renderToHTML(document: Node): String {
        return htmlRenderer.render(document)
    }

    private fun hentCss(format: DocFormat): String {
        return try {
            val cssPath = getCss(contentRoot, format)
            Files.readString(cssPath, UTF_8)
        } catch (e: IOException) {
            throw RuntimeException("Kan ikke hente $format.css", e)
        }
    }

    private val markdownExtensions: List<Extension>
        get() = listOf(TablesExtension.create())

    private val markdownToHtmlParser: Parser
        get() = Parser.builder()
            .extensions(markdownExtensions)
            .build()
    private val htmlRenderer: HtmlRenderer
        get() = HtmlRenderer.builder()
            .extensions(markdownExtensions)
            .build()
    companion object {
        private val UTF_8 = StandardCharsets.UTF_8
        @get:Throws(IOException::class)
        val colorProfile: ByteArray
            get() {
                val cpr = ClassPathResource("sRGB2014.icc")
                return FileCopyUtils.copyToByteArray(cpr.inputStream)
            }

    }

    init {
        XRLog.setLoggingEnabled(false)
    }

    inner class TTFontSupplier(fontName: String) : FSSupplier<InputStream> {
        private val fontPath = "$contentRoot/fonts/$fontName"
        override fun supply(): InputStream {
            return try {
                FileInputStream(Paths.get(fontPath).toFile())
            } catch (e: Exception) {
                throw RuntimeException("Error loading font: $fontPath", e)
            }
        }
    }
}
