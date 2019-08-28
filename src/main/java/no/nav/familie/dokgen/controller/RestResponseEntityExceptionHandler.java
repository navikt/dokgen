package no.nav.familie.dokgen.controller;

import no.nav.familie.dokgen.feil.DokgenIkkeFunnetException;
import no.nav.familie.dokgen.feil.DokgenValideringException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class RestResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(RestResponseEntityExceptionHandler.class);

    @ExceptionHandler({IllegalArgumentException.class})
    protected Object handleIllegalArgumentException(IllegalArgumentException ex, ServletWebRequest req) {
        return ResponseEntity
                .badRequest()
                .body(lagErrorBody(HttpStatus.BAD_REQUEST, ex, req));
    }

    @ExceptionHandler({DokgenValideringException.class})
    protected Object handleDokgenValideringException(DokgenValideringException ex, ServletWebRequest req) {
        Map<String, Object> body = lagErrorBody(HttpStatus.BAD_REQUEST, ex, req);
        body.put("valideringsfeil", ex.getValideringsFeil());
        return ResponseEntity
                .badRequest()
                .body(body);
    }

    @ExceptionHandler({RuntimeException.class})
    protected Object handleRuntimeException(RuntimeException ex, ServletWebRequest req) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(lagErrorBody(HttpStatus.INTERNAL_SERVER_ERROR, ex, req));
    }

    @ExceptionHandler({DokgenIkkeFunnetException.class})
    protected Object handleDokgenIkkeFunnetExceptin(DokgenIkkeFunnetException ex, ServletWebRequest req) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(lagErrorBody(HttpStatus.NOT_FOUND, ex, req));
    }

    private Map<String, Object> lagErrorBody(HttpStatus status, RuntimeException ex, ServletWebRequest req) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", ex.getMessage());
        body.put("type", ex.getClass().getSimpleName() );
        body.put("path", req.getRequest().getRequestURI());

        LOG.warn("En feil har oppstått " + body, ex);
        return body;
    }

}