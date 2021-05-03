package org.indie.isabella.permahub.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.indie.isabella.permahub.model.http.response.ErrorData
import org.indie.isabella.permahub.model.http.response.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.authentication.InsufficientAuthenticationException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import java.io.IOException
import java.io.Serializable
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@Component
class JwtAuthenticationEntryPoint: AuthenticationEntryPoint, Serializable {

    private val objectMapper = ObjectMapper()

    private val logger = LogFactory.getLog(javaClass)

    @Throws(IOException::class)
    override fun commence(
        request: HttpServletRequest?, response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        logger.debug(authException.message, authException)
        var message = authException.message
        if (authException is InsufficientAuthenticationException) {
            message = "Full authentication is required"
        }
        response.setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.writer.print(objectMapper.writeValueAsString(ErrorResponse(ErrorData(authException::class.simpleName.toString(), message))))
    }
}