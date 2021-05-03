package org.indie.isabella.permahub.model

import com.mongodb.lang.Nullable

class Area(country: String) {
    constructor(country: String, region: String) : this(country) {
        this.region = region
    }
    @Nullable
    var country: String? = country
    @Nullable
    var region: String? = null
}