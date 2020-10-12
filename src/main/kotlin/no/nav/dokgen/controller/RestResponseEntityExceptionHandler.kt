package no.nav.dokgen.controller

import no.nav.dokgen.exceptions.DokgenNotFoundException
import no.nav.dokgen.exceptions.DokgenValidationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.ServletWebRequest
import java.time.LocalDateTime
import java.util.*

@ControllerAdvice
class RestResponseEntityExceptionHandler {
    @ExceptionHandler(IllegalArgumentException::class)
    protected fun handleIllegalArgumentException(ex: IllegalArgumentException, req: ServletWebRequest): Any {
        return ResponseEntity
            .badRequest()
            .body<Map<String, Any?>>(lagErrorBody(HttpStatus.BAD_REQUEST, ex, req))
    }

    @ExceptionHandler(DokgenValidationException::class)
    protected fun handleDokgenValideringException(ex: DokgenValidationException, req: ServletWebRequest): Any {
        val body = lagErrorBody(HttpStatus.BAD_REQUEST, ex, req)
        body["valideringsfeil"] = ex.validationErrors
        return ResponseEntity
            .badRequest()
            .body<Map<String, Any?>>(body)
    }

    @ExceptionHandler(RuntimeException::class)
    protected fun handleRuntimeException(ex: RuntimeException, req: ServletWebRequest): Any {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body<Map<String, Any?>>(lagErrorBody(HttpStatus.INTERNAL_SERVER_ERROR, ex, req))
    }

    @ExceptionHandler(DokgenNotFoundException::class)
    protected fun handleDokgenIkkeFunnetExceptin(ex: DokgenNotFoundException, req: ServletWebRequest): Any {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body<Map<String, Any?>>(lagErrorBody(HttpStatus.NOT_FOUND, ex, req))
    }

    private fun lagErrorBody(
        status: HttpStatus,
        ex: RuntimeException,
        req: ServletWebRequest
    ): MutableMap<String, Any?> {
        val body: MutableMap<String, Any?> = LinkedHashMap()
        body["timestamp"] = LocalDateTime.now().toString()
        body["status"] = status.value()
        body["error"] = status.reasonPhrase
        body["type"] = ex.javaClass.simpleName
        body["path"] = req.request.requestURI
        LOG.error("En feil har oppstått $body")
        body["message"] = ex.message
        secureLogger.error("En feil har oppstått $body", ex)
        return body
    }

    companion object {
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
        private val LOG = LoggerFactory.getLogger(RestResponseEntityExceptionHandler::class.java)
    }
}