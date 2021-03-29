package org.indie.isabella.permahub.controller.handler

import com.fasterxml.jackson.databind.ObjectMapper
import org.indie.isabella.permahub.model.http.response.ErrorData
import org.indie.isabella.permahub.model.http.response.ErrorResponse
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations
import org.springframework.http.HttpStatus
import org.springframework.http.server.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.cors.DefaultCorsProcessor
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.io.IOException

@Component
class CustomWebMvcRegistrations: WebMvcRegistrations {

    private val objectMapper = ObjectMapper()

    override open fun getRequestMappingHandlerMapping(): RequestMappingHandlerMapping? {
        val rhm = RequestMappingHandlerMapping()
        rhm.corsProcessor = object : DefaultCorsProcessor() {
            @Throws(IOException::class)
            protected override fun rejectRequest(response: ServerHttpResponse) {
                // DO WHATEVER YOU WANT
                response.setStatusCode(HttpStatus.FORBIDDEN)
                response.body.write(objectMapper.writeValueAsBytes(ErrorResponse(ErrorData("InvalidCORS" , "Invalid CORS request"))))
                response.flush()
            }
        }
        return rhm
    }
}