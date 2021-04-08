package org.indie.isabella.permahub.services

import org.indie.isabella.permahub.exception.AccessDeniedException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import java.util.*


@Service
class JwtUserDetailsService: UserDetailsService {

    @Autowired
    private lateinit var userService: UserService

    @Throws(UsernameNotFoundException::class)
    override fun loadUserByUsername(username: String): UserDetails {
        val userOptional = userService.getUserByEmail(username)
        if (userOptional.isEmpty) throw UsernameNotFoundException("user with email `$username` doesn't exist")
        else if (!userOptional.get().verified) throw AccessDeniedException("Please verify your email")
        else return User(
            userOptional.get().email, userOptional.get().password,
            ArrayList()
        )
    }

}