package org.indie.isabella.permahub.services

import org.indie.isabella.permahub.entity.User
import org.indie.isabella.permahub.entity.repository.UserRepository
import org.indie.isabella.permahub.exception.BadInputException
import org.indie.isabella.permahub.model.http.request.UserData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.*
import java.util.regex.Pattern

@Service
class UserService {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var mailService: MailService

    @Value("\${permahub.public.frontend.url}")
    private lateinit var PUBLIC_FRONT_END_URL: String


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
        private const val VERIFICATION_EMAIL_SUBJECT = "PermaHub sign up verification"
    }


    fun createUser(userData: UserData): User {
        validate(userData)
        var user = User(
            userData.email,
            passwordEncoder.encode(userData.password),
            UUID.randomUUID()
        )
        user = userRepository.save(user)
        sendVerificationEmail(user)

        return user
    }

    private fun sendVerificationEmail(user: User) {
        mailService.send(
            user.email,
            VERIFICATION_EMAIL_SUBJECT,
            "Thank you for joining PermaHub!<br>" +
                    "Please click this " +
                    "<a href='${PUBLIC_FRONT_END_URL}/users/verify?code=${user.verificationCode}' target='_blank'>link</a>" +
                    " to verify your email."
        )
    }

    private fun validate(userData: UserData) {
        if (!EMAIL_REGEX.matcher(userData.email).matches())
            throw BadInputException("Email should be _@_._")
        if (userData.password.length < PASSWORD_MIN_LENGTH)
            throw BadInputException("Password should be $PASSWORD_MIN_LENGTH characters at least")
    }
}