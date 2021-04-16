package org.indie.isabella.permahub.controller.pri

import org.indie.isabella.permahub.model.http.response.SuccessResponse
import org.indie.isabella.permahub.services.UserService
import org.indie.isabella.permahub.utils.JwtTokenUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/users")
@CrossOrigin(value = ["\${permahub.private.frontend.url}"],
    allowCredentials = "true", methods = [RequestMethod.OPTIONS, RequestMethod.GET],
            maxAge = 1800)
class UserController {

    @Autowired
    private lateinit var userService: UserService

    @GetMapping()
    fun get(@RequestHeader(JwtTokenUtil.AUTHORIZATION_FIELD) jwt: String): ResponseEntity<SuccessResponse> {
        return ResponseEntity.status(HttpStatus.OK).body(SuccessResponse(
                userService.getUserFromToken(jwt)
        ))
    }
}