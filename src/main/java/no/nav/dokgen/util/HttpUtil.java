package no.nav.dokgen.util;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

public class HttpUtil {
    public static HttpHeaders genHeaders(DocFormat format, String malNavn, boolean download) {
        if (format.equals(DocFormat.HTML)) {
            return genHtmlHeaders();
        } else if (format.equals(DocFormat.PDF)) {
            return genPdfHeaders(malNavn, download);
        }
        return null;
    }

    public static HttpHeaders genHtmlHeaders() {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.put("charset", Collections.singletonList(StandardCharsets.UTF_8.toString()));
        HttpHeaders headers = new HttpHeaders(map);
        headers.setContentType(MediaType.TEXT_HTML);
        return headers;
    }

    public static HttpHeaders genPdfHeaders(String malNavn, boolean download) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        String filename = malNavn + ".pdf";
        ContentDisposition.Builder contentDisposition = ContentDisposition.builder(download ? "attachment" : "inline");
        contentDisposition.filename(filename);
        headers.setContentDisposition(contentDisposition.build());
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        return headers;
    }
}
