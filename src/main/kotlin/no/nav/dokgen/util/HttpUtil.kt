package no.nav.dokgen.util

import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import java.nio.charset.StandardCharsets

object HttpUtil {
    fun genHeaders(format: DocFormat, malNavn: String, download: Boolean): HttpHeaders? {
        return when(format){
            DocFormat.HTML -> genHtmlHeaders()
            DocFormat.PDF -> genPdfHeaders(malNavn, download)
            else -> null
        }
    }

    fun genHtmlHeaders(): HttpHeaders {
        val map: MultiValueMap<String, String> = LinkedMultiValueMap()
        map["charset"] = listOf(StandardCharsets.UTF_8.toString())
        val headers = HttpHeaders(map)
        headers.contentType = MediaType.TEXT_HTML
        return headers
    }

    fun genPdfHeaders(malNavn: String, download: Boolean): HttpHeaders {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_PDF
        val filename = "$malNavn.pdf"
        val contentDisposition = ContentDisposition.builder(if (download) "attachment" else "inline")
        contentDisposition.filename(filename)
        headers.contentDisposition = contentDisposition.build()
        headers.cacheControl = "must-revalidate, post-check=0, pre-check=0"
        return headers
    }
}