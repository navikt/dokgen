package no.nav.dokgen.configuration

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class LoggerInterceptor : HandlerInterceptorAdapter() {
    @Throws(Exception::class)
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        LOG.info("[pre-handle]- " + request.method + " " + request.requestURI)
        return super.preHandle(request, response, handler)
    }

    @Throws(Exception::class)
    override fun postHandle(
        request: HttpServletRequest, response: HttpServletResponse, handler: Any,
        modelAndView: ModelAndView?
    ) {
        LOG.info("[post-handle] - " + request.method + " " + request.requestURI + " " + response.status)
        super.postHandle(request, response, handler, modelAndView)
    }

    private val LOG = LoggerFactory.getLogger(LoggerInterceptor::class.java)
}