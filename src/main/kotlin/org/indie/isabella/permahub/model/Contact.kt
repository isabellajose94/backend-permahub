package org.indie.isabella.permahub.model

import com.mongodb.lang.Nullable

class Contact(private: String, public: String) {
    @Nullable
    var private: String? = private
    @Nullable
    var public: String? = public
}