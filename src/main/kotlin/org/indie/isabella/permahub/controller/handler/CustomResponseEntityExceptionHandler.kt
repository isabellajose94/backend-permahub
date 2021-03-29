package org.indie.isabella.permahub.controller.handler

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import org.indie.isabella.permahub.exception.BadInputException
import org.indie.isabella.permahub.model.http.response.ErrorData
import org.indie.isabella.permahub.model.http.response.ErrorResponse
import org.indie.isabella.permahub.utils.StringExceptionUtil
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
class CustomResponseEntityExceptionHandler: ResponseEntityExceptionHandler() {
    private val objectMapper = ObjectMapper().configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

    protected override fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatus,
        request: WebRequest
    ): ResponseEntity<Any> {
        logger.info(ex)
        val message = ex.localizedMessage.split(":")[0]
        return handleExceptionInternal(ex, message, headers, status, request)
    }

    @ExceptionHandler(value = [
        BadInputException::class,
        DuplicateKeyException::class,
    ])
    final fun handleExceptionCustom(ex: Exception, request: WebRequest) : ResponseEntity<Any> {
        logger.debug(ex)
        var status = HttpStatus.INTERNAL_SERVER_ERROR
        var message = ex.message.toString()

        if (ex is BadInputException) {
            status = HttpStatus.BAD_REQUEST
        }
        if (ex is DuplicateKeyException){
            status = HttpStatus.BAD_REQUEST

            val collection = StringExceptionUtil.getValue(message, "collection", " ", ).split(".")[1]
            val duplicateKey = StringExceptionUtil.getValue(message, "dup key", "}", true)
            val duplicateKeyMap = objectMapper.readValue(duplicateKey, Map::class.java)
            val duplicateKeyMessage = duplicateKeyMap.map{(key, value) -> "$key `$value`"}.joinToString(", ")

            message = "$collection with $duplicateKeyMessage already exist"
        }

        return handleExceptionInternal(ex, message, HttpHeaders(), status, request)

    }

    protected override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        status: HttpStatus,
        request: WebRequest
    ): ResponseEntity<Any> {
        val response = ErrorResponse(ErrorData(ex::class.simpleName.toString(), body))
        return super.handleExceptionInternal(ex, response, headers, status, request)
    }
}