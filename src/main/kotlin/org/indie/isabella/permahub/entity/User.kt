package org.indie.isabella.permahub.entity

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.mongodb.lang.Nullable
import org.indie.isabella.permahub.model.Area
import org.indie.isabella.permahub.model.Contact
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

@JsonIgnoreProperties("id", "password", "verificationCode")
@Document
data class User(
    @Indexed(unique = true)
    var email: String,
    var password: String,
    @Indexed(unique = true)
    var verificationCode: UUID
) {
    @Id
    lateinit var id: String

    @CreatedDate
    lateinit var createdDate: Date

    @LastModifiedDate
    lateinit var lastModifiedDate: Date

    var verified: Boolean = false

    @Nullable
    var name: String? = null

    @Nullable
    var headline: String? = null

    @Nullable
    var about: String? = null

    @Nullable
    var type: String? = null

    @Nullable
    var area: Area? = null

    @Nullable
    var contact: Contact? = null

}