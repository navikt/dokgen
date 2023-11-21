package no.nav.dokgen.configuration

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView

@Component
class LoggerInterceptor : HandlerInterceptor {
    private val log = LoggerFactory.getLogger(LoggerInterceptor::class.java)

    @Throws(Exception::class)
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        log.info("[pre-handle] - {} {}", request.method, request.requestURI)
        return true
    }

    @Throws(Exception::class)
    override fun postHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        modelAndView: ModelAndView?
    ) {
        log.info("[post-handle] - {} {} {}", request.method, request.requestURI, response.status)
    }
}