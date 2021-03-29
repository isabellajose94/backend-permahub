package org.indie.isabella.permahub.controller.public

import org.assertj.core.api.Assertions
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
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = [MongoInitializer::class])
@ActiveProfiles(value = ["test"])
@DisplayName("Public User Controller Test")
class PublicUserControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    private val objectMapper: ObjectMapper = ObjectMapper()

    @Value("\${permahub.public.frontend.url}")
    private lateinit var PUBLIC_FRONT_END_URL: String

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    @DisplayName("Request contains email and password")
    inner class RequestContainsEmailAndPassword() {
        private lateinit var result: ResultActions

        @BeforeAll
        fun triggerEvent() {
            result = mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .post("/public/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            objectMapper.writeValueAsString(
                                UserData(
                                    "email@mail.co",
                                    "the_password"
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
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success.email", CoreMatchers.`is`("email@mail.co")))
                .andExpect(jsonPath("$.success.password").doesNotExist())
                .andDo(MockMvcResultHandlers.print());
        }

        @Test
        fun shouldSaveUser() {
            val encoder = BCryptPasswordEncoder(16)
            val users = userRepository.findAll()
            Assertions.assertThat(users).hasSize(1)
            Assertions.assertThat(users[0].email).isEqualTo("email@mail.co")
            Assertions.assertThat(encoder.matches("the_password", users[0].password)).isTrue
            Assertions.assertThat(users[0].createdDate).isNotNull
            Assertions.assertThat(users[0].lastModifiedDate).isNotNull
        }
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    @DisplayName("Empty request")
    inner class EmptyRequest() {
        private lateinit var result: ResultActions

        @BeforeAll
        fun triggerEvent() {
            result = mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .post("/public/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Origin", PUBLIC_FRONT_END_URL)
                )
        }

        @AfterAll
        fun clearDb() {
            userRepository.deleteAll()
        }

        @Test
        fun shouldReturn400() {
            result
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.type", CoreMatchers.`is`("HttpMessageNotReadableException")))
                .andExpect(jsonPath("$.error.message", CoreMatchers.`is`("Required request body is missing")))
        }

        @Test
        fun shouldNotSave() {
            Assertions.assertThat(userRepository.count()).isEqualTo(0)
        }
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    @DisplayName("Invalid email")
    inner class InvalidEmail() {
        private lateinit var result: ResultActions

        @BeforeAll
        fun triggerEvent() {
            result = mockMvc.perform(
                MockMvcRequestBuilders
                    .post("/public/api/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\n" +
                                "  \"email\": \"sddd\",\n" +
                                "  \"password\": \"12341234\"\n" +
                                "}"
                    )
                    .header("Origin", PUBLIC_FRONT_END_URL)
            )
        }

        @AfterAll
        fun clearDb() {
            userRepository.deleteAll()
        }


        @Test
        fun shouldReturn400() {
            result
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.type", CoreMatchers.`is`("BadInputException")))
                .andExpect(jsonPath("$.error.message", CoreMatchers.`is`("Email should be _@_._")))
        }

        @Test
        fun shouldNotSave() {
            Assertions.assertThat(userRepository.count()).isEqualTo(0)
        }
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    @DisplayName("Invalid password")
    inner class InvalidPassword() {
        private lateinit var result: ResultActions

        @BeforeAll
        fun triggerEvent() {
            result = mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .post("/public/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\n" +
                                    "  \"email\": \"kjd@dfk.c\",\n" +
                                    "  \"password\": \"ddd\"\n" +
                                    "}"
                        )
                        .header("Origin", PUBLIC_FRONT_END_URL)
                )
        }

        @AfterAll
        fun clearDb() {
            userRepository.deleteAll()
        }

        @Test
        fun shouldReturn400() {
            result
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.type", CoreMatchers.`is`("BadInputException")))
                .andExpect(
                    jsonPath(
                        "$.error.message",
                        CoreMatchers.`is`("Password should be 8 characters at least")
                    )
                )
        }

        @Test
        fun shouldNotSave() {
            Assertions.assertThat(userRepository.count()).isEqualTo(0)
        }
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    @DisplayName("Existing client")
    inner class RegisterWithExistingClient() {
        private lateinit var result: ResultActions

        @BeforeAll
        fun triggerEvent() {
            userRepository.save(User("existing@client.co", "password"))
            result = mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .post("/public/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\n" +
                                    "  \"email\": \"existing@client.co\",\n" +
                                    "  \"password\": \"12341234\"\n" +
                                    "}"
                        )
                        .header("Origin", PUBLIC_FRONT_END_URL)
                )
        }

        @AfterAll
        fun clearDb() {
            userRepository.deleteAll()
        }

        @Test
        fun shouldReturn400() {
            result
                .andExpect(status().isBadRequest)
                .andExpect(
                    jsonPath(
                        "$.error.type",
                        CoreMatchers.`is`("DuplicateKeyException")
                    )
                )
                .andExpect(
                    jsonPath(
                        "$.error.message",
                        CoreMatchers.`is`("user with email `existing@client.co` already exist")
                    )
                )
        }

        @Test
        fun shouldNotSave() {
            Assertions.assertThat(userRepository.count()).isEqualTo(1)
        }
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    @DisplayName("Unauthorize CORS origin")
    inner class UnauthorizeCORSOrigin() {
        private lateinit var result: ResultActions

        @BeforeAll
        fun triggerEvent() {
            result = mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .post("/public/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\n" +
                                    "  \"email\": \"existing@client.co\",\n" +
                                    "  \"password\": \"12341234\"\n" +
                                    "}"
                        )
                        .header("Origin", "Invalid CORS origin")
                )
        }

        @AfterAll
        fun clearDb() {
            userRepository.deleteAll()
        }

        @Test
        fun shouldReturn403() {
            result
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isForbidden)
                .andExpect(
                    jsonPath(
                        "$.error.type",
                        CoreMatchers.`is`("InvalidCORS")
                    )
                )
                .andExpect(
                    jsonPath(
                        "$.error.message",
                        CoreMatchers.`is`("Invalid CORS request")
                    )
                )
        }

        @Test
        fun shouldNotSave() {
            Assertions.assertThat(userRepository.count()).isEqualTo(0)
        }
    }
}