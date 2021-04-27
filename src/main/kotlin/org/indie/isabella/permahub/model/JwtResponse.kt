package org.indie.isabella.permahub.model

import java.io.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class JwtResponse : Serializable {
    var accessToken: String
    var refreshToken: String
    var expiredAt: String

    constructor(accessToken: String,
                refreshToken: String,
                expiredAt: LocalDateTime) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken

        this.expiredAt = expiredAt.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_INSTANT)
    }

}