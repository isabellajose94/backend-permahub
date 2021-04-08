package org.indie.isabella.permahub.services

import org.indie.isabella.permahub.model.JwtResponse
import org.indie.isabella.permahub.model.http.request.UserData
import org.indie.isabella.permahub.utils.JwtTokenUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service
import java.util.*

@Service
class AuthenticationService {
    @Autowired
    private lateinit var authenticationManager: AuthenticationManager

    @Autowired
    private lateinit var jwtTokenUtil: JwtTokenUtil

    @Autowired
    private lateinit var jwtInMemoryUserDetailsService: UserDetailsService

    @Throws(AuthenticationException::class)
    fun authenticate(userData: UserData): JwtResponse {
        Objects.requireNonNull(userData.email)
        Objects.requireNonNull(userData.password)
        authenticationManager.authenticate(UsernamePasswordAuthenticationToken(userData.email, userData.password))
        val userDetails: UserDetails = jwtInMemoryUserDetailsService
                .loadUserByUsername(userData.email)
        val token: String = jwtTokenUtil.generateToken(userDetails)
        return JwtResponse(token)

    }
}