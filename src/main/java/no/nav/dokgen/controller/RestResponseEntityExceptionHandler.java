package no.nav.dokgen.controller;

import no.nav.dokgen.exceptions.DokgenNotFoundException;
import no.nav.dokgen.exceptions.DokgenValidationException;
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
    private static final Logger secureLogger = LoggerFactory.getLogger("secureLogger");
    private static final Logger LOG = LoggerFactory.getLogger(RestResponseEntityExceptionHandler.class);

    @ExceptionHandler({IllegalArgumentException.class})
    protected Object handleIllegalArgumentException(IllegalArgumentException ex, ServletWebRequest req) {
        return ResponseEntity
                .badRequest()
                .body(lagErrorBody(HttpStatus.BAD_REQUEST, ex, req));
    }

    @ExceptionHandler({DokgenValidationException.class})
    protected Object handleDokgenValideringException(DokgenValidationException ex, ServletWebRequest req) {
        Map<String, Object> body = lagErrorBody(HttpStatus.BAD_REQUEST, ex, req);
        body.put("valideringsfeil", ex.getValidationErrors());
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

    @ExceptionHandler({DokgenNotFoundException.class})
    protected Object handleDokgenIkkeFunnetExceptin(DokgenNotFoundException ex, ServletWebRequest req) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(lagErrorBody(HttpStatus.NOT_FOUND, ex, req));
    }

    private Map<String, Object> lagErrorBody(HttpStatus status, RuntimeException ex, ServletWebRequest req) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("type", ex.getClass().getSimpleName() );
        body.put("path", req.getRequest().getRequestURI());

        LOG.error("En feil har oppstått " + body);

        body.put("message", ex.getMessage());
        secureLogger.error("En feil har oppstått " + body, ex);

        return body;
    }

}