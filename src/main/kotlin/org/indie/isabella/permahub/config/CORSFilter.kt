package org.indie.isabella.permahub.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class CORSFilter : OncePerRequestFilter() {

    @Value("\${permahub.private.frontend.url}")
    private lateinit var PERMAHUB_PRIVATE_FRONTEND_URL: String

    override protected fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        if (request.getHeader("Origin") == PERMAHUB_PRIVATE_FRONTEND_URL
            && "OPTIONS".equals(request.method, true)) {
            response.setHeader("Access-Control-Allow-Origin", PERMAHUB_PRIVATE_FRONTEND_URL)
            response.setHeader("Access-Control-Allow-Methods", "OPTIONS")
            response.setHeader("Access-Control-Allow-Headers", "*")
            response.setHeader("Access-Control-Allow-Credentials", "true")
            response.setHeader("Access-Control-Max-Age", "180")
            response.status = HttpServletResponse.SC_OK
        } else {
            chain.doFilter(request, response)
        }

    }
}