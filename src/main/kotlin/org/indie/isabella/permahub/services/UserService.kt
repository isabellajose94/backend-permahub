package org.indie.isabella.permahub.services

import org.indie.isabella.permahub.entity.User
import org.indie.isabella.permahub.entity.repository.UserRepository
import org.indie.isabella.permahub.exception.BadInputException
import org.indie.isabella.permahub.model.http.request.UserData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.regex.Pattern

@Service
class UserService {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    companion object {
        val EMAIL_REGEX: Pattern = Pattern.compile(
            "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                    "\\@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                    ")+"
        )
        private const val PASSWORD_MIN_LENGTH = 8
    }


    fun createUser(userData: UserData): User {
        validate(userData)
        var user = User(
            userData.email,
            passwordEncoder.encode(userData.password)
        )

        return userRepository.save(user)
    }

    private fun validate(userData: UserData) {
        if (!EMAIL_REGEX.matcher(userData.email).matches())
            throw BadInputException("Email should be _@_._")
        if (userData.password.length < PASSWORD_MIN_LENGTH)
            throw BadInputException("Password should be $PASSWORD_MIN_LENGTH characters at least")
    }
}