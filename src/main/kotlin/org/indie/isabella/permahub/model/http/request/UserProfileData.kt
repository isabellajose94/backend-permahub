package org.indie.isabella.permahub.model.http.request

import org.indie.isabella.permahub.model.Area
import org.indie.isabella.permahub.model.Contact


data class UserProfileData(
    val name: String?,
    val headline: String?,
    val about: String?,
    val type: String?,
    val area: Area?,
    val contact: Contact?
)