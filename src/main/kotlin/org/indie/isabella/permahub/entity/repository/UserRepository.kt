package org.indie.isabella.permahub.entity.repository

import org.indie.isabella.permahub.entity.User
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository: MongoRepository<User, String> {
}