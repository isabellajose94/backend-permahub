package org.indie.isabella.permahub.controller.pub.user

import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.CoreMatchers
import org.indie.isabella.permahub.MongoInitializer
import org.indie.isabella.permahub.entity.User
import org.indie.isabella.permahub.entity.repository.UserRepository
import org.indie.isabella.permahub.model.http.request.UserData
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = [MongoInitializer::class])
@ActiveProfiles(value = ["test"])
@DisplayName("Authenticate Public User Controller Test")
class AuthenticateUserControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    private val objectMapper: ObjectMapper = ObjectMapper()

    @Value("\${permahub.public.frontend.url}")
    private lateinit var PUBLIC_FRONT_END_URL: String

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    @DisplayName("Non existent email")
    inner class NonExistentEmail() {
        private lateinit var result: ResultActions

        @BeforeAll
        fun triggerEvent() {
            result = mockMvc.perform(
                    MockMvcRequestBuilders
                            .post("/public/api/users/authenticate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                    objectMapper.writeValueAsString(
                                            UserData(
                                                    "non_existent@mail.co",
                                                    "the_password"
                                            )
                                    )
                            )
                            .header("Origin", PUBLIC_FRONT_END_URL)
            )
        }

        @Test
        fun shouldReturn401() {
            result
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized)
                    .andExpect(MockMvcResultMatchers.jsonPath("$.error.type", CoreMatchers.`is`("BadCredentialsException")))
                    .andExpect(
                            MockMvcResultMatchers.jsonPath(
                                    "$.error.message",
                                    CoreMatchers.`is`("Invalid email or password")
                            )
                    )
        }
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    @DisplayName("Existent email wrong password")
    inner class ExistentEmailWrongPassword() {
        private lateinit var result: ResultActions

        @BeforeAll
        fun triggerEvent() {
            val user = User(
                    "existing@client.co",
                    BCryptPasswordEncoder().encode("password"),
                    UUID.randomUUID())
            user.verified = true
            userRepository.save(user)
            result = mockMvc.perform(
                    MockMvcRequestBuilders
                            .post("/public/api/users/authenticate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                    objectMapper.writeValueAsString(
                                            UserData(
                                                    "existing@client.co",
                                                    "wrong_password"
                                            )
                                    )
                            )
                            .header("Origin", PUBLIC_FRONT_END_URL)
            )
        }

        @AfterAll
        fun clearDb() {
            userRepository.deleteAll()
        }

        @Test
        fun shouldReturn401() {
            result
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized)
                    .andExpect(MockMvcResultMatchers.jsonPath("$.error.type", CoreMatchers.`is`("BadCredentialsException")))
                    .andExpect(
                            MockMvcResultMatchers.jsonPath(
                                    "$.error.message",
                                    CoreMatchers.`is`("Invalid email or password")
                            )
                    )
        }
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    @DisplayName("Correct credentials but not verified")
    inner class CorrectCredentialsButNotVerified() {
        private lateinit var result: ResultActions

        @BeforeAll
        fun triggerEvent() {
            userRepository.save(User("existing@client.co", BCryptPasswordEncoder().encode("password"),UUID.randomUUID()))
            result = mockMvc.perform(
                    MockMvcRequestBuilders
                            .post("/public/api/users/authenticate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                    objectMapper.writeValueAsString(
                                            UserData(
                                                    "existing@client.co",
                                                    "password"
                                            )
                                    )
                            )
                            .header("Origin", PUBLIC_FRONT_END_URL)
            )
        }

        @AfterAll
        fun clearDb() {
            userRepository.deleteAll()
        }

        @Test
        fun shouldReturn401() {
            result
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized)

                    .andExpect(MockMvcResultMatchers.jsonPath("$.error.type", CoreMatchers.`is`("InternalAuthenticationServiceException")))
                    .andExpect(
                            MockMvcResultMatchers.jsonPath(
                                    "$.error.message",
                                    CoreMatchers.`is`("Please verify your email")
                            )
                    )
        }
    }


    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    @DisplayName("Correct credentials and verified")
    inner class CorrectCredentialsAndVerified() {
        private lateinit var result: ResultActions

        @BeforeAll
        fun triggerEvent() {
            val user = User("existing@client.co", BCryptPasswordEncoder().encode("password"), UUID.randomUUID())
            user.verified = true
            userRepository.save(user)

            result = mockMvc.perform(
                    MockMvcRequestBuilders
                            .post("/public/api/users/authenticate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                    objectMapper.writeValueAsString(
                                            UserData(
                                                    "existing@client.co",
                                                    "password"
                                            )
                                    )
                            )
                            .header("Origin", PUBLIC_FRONT_END_URL)
            )
        }

        @AfterAll
        fun clearDb() {
            userRepository.deleteAll()
        }

        @Test
        fun shouldReturn201() {
            result
                    .andExpect(MockMvcResultMatchers.status().isCreated)
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success.token", CoreMatchers.any(String::class.java)))
        }
    }
}