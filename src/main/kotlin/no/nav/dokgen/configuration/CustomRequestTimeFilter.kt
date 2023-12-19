package no.nav.dokgen.configuration

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.util.StopWatch
import java.io.IOException

// dupliserer no.nav.familie.log.filter.RequestTimeFilter ettersom shouldNotFilter ikke kan overrides
class CustomRequestTimeFilter : Filter {
    @Throws(IOException::class, ServletException::class)
    override fun doFilter(
        servletRequest: ServletRequest,
        servletResponse: ServletResponse,
        filterChain: FilterChain,
    ) {
        val response = servletResponse as HttpServletResponse
        val request = servletRequest as HttpServletRequest
        val timer = StopWatch()
        try {
            timer.start()
            filterChain.doFilter(servletRequest, servletResponse)
        } finally {
            timer.stop()
            log(request, response.status, timer)
        }
    }

    private fun log(
        request: HttpServletRequest,
        code: Int,
        timer: StopWatch,
    ) {
        if (HttpStatus.valueOf(code).isError) {
            LOG.warn("{} - {} - ({}). Dette tok {}ms", request.method, request.requestURI, code, timer.totalTimeMillis)
        } else {
            if (!shouldNotFilter(request.requestURI)) {
                LOG.info(
                    "{} - {} - ({}). Dette tok {}ms",
                    request.method,
                    request.requestURI,
                    code,
                    timer.totalTimeMillis
                )
            }
        }
    }

     fun shouldNotFilter(uri: String): Boolean {
         return uri.contains("/internal")
                 || uri == "/api/ping"
                 || uri.contains("/swagger")
                 || uri.contains("/actuator")
                 || uri.contains("/favicon.ico")
     }

    companion object {
        private val LOG = LoggerFactory.getLogger(CustomRequestTimeFilter::class.java)
    }
}