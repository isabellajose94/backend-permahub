package org.indie.isabella.permahub.entity

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

@JsonIgnoreProperties("password")
@Document
data class User(
    @Indexed(unique = true)
    var email: String,
    var password:String
) {
    @Id
    lateinit var id: String

    @CreatedDate
    lateinit var createdDate: Date

    @LastModifiedDate
    lateinit var lastModifiedDate: Date
}