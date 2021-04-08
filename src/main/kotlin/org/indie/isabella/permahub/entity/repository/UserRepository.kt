package org.indie.isabella.permahub.entity.repository

import org.indie.isabella.permahub.entity.User
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository: MongoRepository<User, String> {
    fun findOneByVerificationCode(verificationCode: UUID): Optional<User>
    fun findOneByEmail(email: String): Optional<User>
}