package org.indie.isabella.permahub.utils

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.impl.DefaultClock
import org.indie.isabella.permahub.model.JwtResponse
import org.indie.isabella.permahub.model.UserStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.io.Serializable
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import java.util.function.Function

@Component
class JwtTokenUtil : Serializable {

    companion object {
        const val AUTHORIZATION_FIELD = "Authorization"
        const val AUTHORIZATION_PREFIX = "Bearer "
        const val JWT_ACCESS_TOKEN_VALIDITY_BY_HOUR = 2L
        const val JWT_REFRESH_TOKEN_VALIDITY_BY_HOUR = 24L
    }

    @Autowired
    private lateinit var defaultClock: DefaultClock

    @Value("\${jwt.access.secret}")
    private lateinit var ACCESS_SECRET: String

    @Value("\${jwt.refresh.secret}")
    private lateinit var REFRESH_SECRET: String

    fun getUsernameFromToken(token: String, isAccessToken: Boolean, bearerRemoved: Boolean): String {
        var filteredToken = token
        if (!bearerRemoved) filteredToken = filteredToken.substring(AUTHORIZATION_PREFIX.length)
        return getUsernameFromToken(filteredToken, isAccessToken)
    }

    fun getUsernameFromToken(token: String?, isAccessToken: Boolean = true): String {
        return getClaimFromToken(token, Claims::getSubject, isAccessToken)
    }

    fun getIssuedAtDateFromToken(token: String?): Date {
        return getClaimFromToken(token, Claims::getIssuedAt)
    }

    fun getExpirationDateFromToken(token: String?): Date {
        return getClaimFromToken(token, Claims::getExpiration)
    }

    fun <T> getClaimFromToken(token: String?, claimsResolver: Function<Claims, T>, isAccessToken: Boolean = true): T {
        val claims = getAllClaimsFromToken(token, isAccessToken)
        return claimsResolver.apply(claims)
    }

    private fun getAllClaimsFromToken(token: String?, isAccessToken: Boolean = true): Claims {
        var secret = ACCESS_SECRET
        if (!isAccessToken) secret = REFRESH_SECRET
        return Jwts.parser().setClock(defaultClock).setSigningKey(secret).parseClaimsJws(token).body
    }

    private fun isTokenExpired(token: String): Boolean? {
        val expiration: Date = getExpirationDateFromToken(token)
        return expiration.before(Date())
    }

    private fun ignoreTokenExpiration(token: String): Boolean {
        // here you specify tokens, for that the expiration is ignored
        return false
    }
    fun buildJwtResponse(userDetails: UserDetails): JwtResponse {
        val issuedAt = LocalDateTime.ofInstant(defaultClock.now().toInstant(), ZoneId.systemDefault())
        val expirationAccessToken = issuedAt.plusHours(JWT_ACCESS_TOKEN_VALIDITY_BY_HOUR)
        val expirationRefreshToken = issuedAt.plusHours(JWT_REFRESH_TOKEN_VALIDITY_BY_HOUR)
        return JwtResponse(
            doGenerateToken(userDetails.username, issuedAt, expirationAccessToken, SignatureAlgorithm.HS512, ACCESS_SECRET),
            doGenerateToken(userDetails.username, issuedAt, expirationRefreshToken, SignatureAlgorithm.HS384, REFRESH_SECRET),
                    expirationAccessToken
        )
    }

    private fun doGenerateToken(subject: String, issuedAt: LocalDateTime, expiration: LocalDateTime, signatureAlgorithm: SignatureAlgorithm, secret: String): String {

        return Jwts.builder().setSubject(subject).setIssuedAt(Date.from(issuedAt.atZone(ZoneId.systemDefault()).toInstant()))
            .setExpiration(Date.from(expiration.atZone(ZoneId.systemDefault()).toInstant()))
            .signWith(signatureAlgorithm, secret).compact()
    }

    fun canTokenBeRefreshed(token: String): Boolean? {
        return !isTokenExpired(token)!! || ignoreTokenExpiration(token)
    }

    fun validateToken(token: String, userDetails: UserDetails): Boolean {
        val username = getUsernameFromToken(token)
        return username == userDetails.username && !isTokenExpired(token)!!
    }
}