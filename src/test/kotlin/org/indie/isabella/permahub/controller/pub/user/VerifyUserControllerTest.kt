package org.indie.isabella.permahub.controller.pub.user

import org.assertj.core.api.Assertions
import org.hamcrest.CoreMatchers
import org.indie.isabella.permahub.config.MongoInitializer
import org.indie.isabella.permahub.entity.User
import org.indie.isabella.permahub.entity.repository.UserRepository
import org.indie.isabella.permahub.model.http.request.VerifyData
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
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = [MongoInitializer::class])
@ActiveProfiles(value = ["test"])
@DisplayName("Verify Public User Controller Test")
class VerifyUserControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    private val objectMapper: ObjectMapper = ObjectMapper()

    @Value("\${permahub.public.frontend.url}")
    private lateinit var PUBLIC_FRONT_END_URL: String

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    @DisplayName("Request is empty")
    inner class RequestIsEmpty() {
        private lateinit var result: ResultActions

        @BeforeAll
        fun triggerEvent() {

            result = mockMvc
                    .perform(
                            MockMvcRequestBuilders
                                    .patch("/public/api/users/verify")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("Origin", PUBLIC_FRONT_END_URL)
                    )
        }


        @Test
        fun shouldReturn400() {
            result
                    .andExpect(MockMvcResultMatchers.status().isBadRequest)
                    .andExpect(MockMvcResultMatchers.jsonPath("$.error.type", CoreMatchers.`is`("HttpMessageNotReadableException")))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.error.message", CoreMatchers.`is`("Required request body is missing")))
        }

    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    @DisplayName("Request has invalid UUID")
    inner class RequestHasInvalidUUID() {
        private lateinit var result: ResultActions

        @BeforeAll
        fun triggerEvent() {
            result = mockMvc
                    .perform(
                            MockMvcRequestBuilders
                                    .patch("/public/api/users/verify")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(
                                            VerifyData("Invalid UUID code")
                                    ))
                                    .header("Origin", PUBLIC_FRONT_END_URL)
                    )
        }

        @Test
        fun shouldReturn400() {
            result
                    .andExpect(MockMvcResultMatchers.status().isBadRequest)
                    .andExpect(MockMvcResultMatchers.jsonPath("$.error.type", CoreMatchers.`is`("BadInputException")))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.error.message", CoreMatchers.`is`("Code should be UUID")))
        }
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    @DisplayName("Request code doesn't match DB")
    inner class RequestCodeDoesntMatchDB() {
        private lateinit var result: ResultActions

        @BeforeAll
        fun triggerEvent() {
            userRepository.save(User("user@mail.co", "password", UUID.randomUUID()))
            result = mockMvc
                    .perform(
                            MockMvcRequestBuilders
                                    .patch("/public/api/users/verify")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(
                                            VerifyData(UUID.randomUUID().toString())
                                    ))
                                    .header("Origin", PUBLIC_FRONT_END_URL)
                    )
        }

        @AfterAll
        fun clearDb() {
            userRepository.deleteAll()
        }

        @Test
        fun shouldReturn404() {
            result
                    .andExpect(MockMvcResultMatchers.status().isNotFound)
                    .andExpect(MockMvcResultMatchers.jsonPath("$.error.type", CoreMatchers.`is`("NotFoundException")))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.error.message", CoreMatchers.`is`("User has not found")))
        }
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    @DisplayName("Request code match DB")
    inner class RequestCodeMatchDB() {
        private lateinit var result: ResultActions

        @BeforeAll
        fun triggerEvent() {
            val code = UUID.randomUUID()
            userRepository.save(User("user@mail.co", "password", code))
            result = mockMvc
                    .perform(
                            MockMvcRequestBuilders
                                    .patch("/public/api/users/verify")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(
                                            VerifyData(code.toString())
                                    ))
                                    .header("Origin", PUBLIC_FRONT_END_URL)
                    )
        }

        @AfterAll
        fun clearDb() {
            userRepository.deleteAll()
        }

        @Test
        fun shouldReturn200() {
            result
                    .andExpect(MockMvcResultMatchers.status().isOk)
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success.email", CoreMatchers.`is`("user@mail.co")))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success.password").doesNotExist())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success.verificationCode").doesNotExist())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success.verified", CoreMatchers.`is`(true)))
                    .andDo(MockMvcResultHandlers.print())
        }

        @Test
        fun shouldUpdateUser() {
            val encoder = BCryptPasswordEncoder(16)
            val users = userRepository.findAll()
            Assertions.assertThat(users).hasSize(1)
            Assertions.assertThat(users[0].email).isEqualTo("user@mail.co")
            Assertions.assertThat(users[0].verified).isEqualTo(true)
        }
    }

}