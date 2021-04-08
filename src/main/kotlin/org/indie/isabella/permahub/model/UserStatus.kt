package org.indie.isabella.permahub.model

import java.util.*

data class UserStatus(val email: String, val issuedAt: Date, val expiration: Date) {
}