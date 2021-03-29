package org.indie.isabella.permahub.controller.public

import org.indie.isabella.permahub.model.http.request.UserData
import org.indie.isabella.permahub.model.http.response.SuccessResponse
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

    @PostMapping("register")
    fun register(@RequestBody userData: UserData): ResponseEntity<SuccessResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse(
            userService.createUser(userData)
        ))
    }
}