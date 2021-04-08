package org.indie.isabella.permahub.controller.pub

import org.indie.isabella.permahub.model.http.request.UserData
import org.indie.isabella.permahub.model.http.request.VerifyData
import org.indie.isabella.permahub.model.http.response.SuccessResponse
import org.indie.isabella.permahub.services.AuthenticationService
import org.indie.isabella.permahub.services.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("public/api/users")
@CrossOrigin(value = ["\${permahub.public.frontend.url}"])
class PublicUserController {
    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var authenticationService: AuthenticationService

    @PostMapping("register")
    fun register(@RequestBody userData: UserData): ResponseEntity<SuccessResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse(
            userService.createUser(userData)
        ))
    }

    @PostMapping("authenticate")
    fun authenticate(@RequestBody userData: UserData): ResponseEntity<SuccessResponse> {
        val jwtResponse = authenticationService.authenticate(userData)
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse(jwtResponse))
    }

    @PatchMapping("verify")
    fun verify(@RequestBody verifyData: VerifyData): ResponseEntity<SuccessResponse> {
        return ResponseEntity.status(HttpStatus.OK).body(SuccessResponse(
                userService.verifyUser(verifyData)
        ))
    }
}