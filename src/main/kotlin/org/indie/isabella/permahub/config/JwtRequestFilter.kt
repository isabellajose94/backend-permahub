package org.indie.isabella.permahub.config

import io.jsonwebtoken.ExpiredJwtException
import org.indie.isabella.permahub.services.JwtUserDetailsService
import org.indie.isabella.permahub.utils.JwtTokenUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@Component
class JwtRequestFilter: OncePerRequestFilter() {

    @Autowired
    private lateinit var jwtUserDetailsService: JwtUserDetailsService

    @Autowired
    private lateinit var jwtTokenUtil: JwtTokenUtil

    @Throws(ServletException::class, IOException::class)
    override protected fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        val requestTokenHeader = request.getHeader(JwtTokenUtil.AUTHORIZATION_FIELD)
        // JWT Token is in the form "Bearer token". Remove Bearer word and get only the Token
        if (requestTokenHeader != null && requestTokenHeader.startsWith(JwtTokenUtil.AUTHORIZATION_PREFIX)) {
            var jwtToken = requestTokenHeader.substring(7)
            try {
                var username = jwtTokenUtil.getUsernameFromToken(jwtToken)
                if (SecurityContextHolder.getContext().authentication == null) {
                    val userDetails: UserDetails = jwtUserDetailsService.loadUserByUsername(username)
                    // if token is valid configure Spring Security to manually set authentication
                    if (jwtTokenUtil.validateToken(jwtToken, userDetails)) {
                        val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.authorities
                        )
                        usernamePasswordAuthenticationToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                        // After setting the Authentication in the context, we specify
                        // that the current user is authenticated. So it passes the Spring Security Configurations successfully.
                        SecurityContextHolder.getContext().authentication = usernamePasswordAuthenticationToken
                    }
                }
            } catch (e: IllegalArgumentException) {
                logger.error("Unable to get JWT Token")
            } catch (e: ExpiredJwtException) {
                logger.info("JWT Token has expired")
            } catch (e: Exception) {
                logger.error(e.message, e)
            }
        } else {
            logger.warn("JWT Token does not begin with Bearer String")
        }


        chain.doFilter(request, response)
    }
}