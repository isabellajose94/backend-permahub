package org.indie.isabella.permahub.model.http.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

data class UserData(val email:String,
                    val password: String) {
}