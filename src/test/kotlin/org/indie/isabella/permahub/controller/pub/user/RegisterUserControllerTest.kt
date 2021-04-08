package org.indie.isabella.permahub.controller.pub.user

import org.assertj.core.api.Assertions
import org.hamcrest.CoreMatchers
import org.indie.isabella.permahub.MongoInitializer
import org.indie.isabella.permahub.entity.User
import org.indie.isabella.permahub.entity.repository.UserRepository
import org.indie.isabella.permahub.model.http.request.UserData
import org.junit.jupiter.api.*
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.MediaType
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper
import java.util.*
import javax.mail.internet.MimeMessage


@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = [MongoInitializer::class])
@ActiveProfiles(value = ["test"])
@DisplayName("Register Public User Controller Test")
class RegisterUserControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    private val objectMapper: ObjectMapper = ObjectMapper()

    @SpyBean
    private lateinit var javaMailSender: JavaMailSender

    @Value("\${permahub.public.frontend.url}")
    private lateinit var PUBLIC_FRONT_END_URL: String

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    @DisplayName("Request contains email and password")
    inner class RequestContainsEmailAndPassword() {
        private lateinit var result: ResultActions
        private val argumentCaptor = ArgumentCaptor.forClass(MimeMessage::class.java)

        @BeforeAll
        fun triggerEvent() {
            doNothing().`when`(javaMailSender).send(argumentCaptor.capture())

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
                .andExpect(jsonPath("$.success.id").doesNotExist())
                .andExpect(jsonPath("$.success.verificationCode").doesNotExist())
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
            Assertions.assertThat(users[0].verificationCode).isNotNull
        }

        @Test
        fun shouldSendVerificationEmail() {
            Assertions.assertThat(argumentCaptor.allValues.size).isEqualTo(1)
            val actualMailMessage = argumentCaptor.value

            Assertions.assertThat(actualMailMessage.allRecipients).hasSize(1)
            Assertions.assertThat(actualMailMessage.allRecipients[0].toString()).isEqualTo("email@mail.co")
            Assertions.assertThat(actualMailMessage.subject).isEqualTo("PermaHub sign up verification")

            val user = userRepository.findAll()[0]
            Assertions.assertThat(actualMailMessage.content).isEqualTo(
                "Thank you for joining PermaHub!<br>" +
                        "Please click this <a href='$PUBLIC_FRONT_END_URL/users/verify/?code=${user.verificationCode}' target='_blank'>link</a> to verify your email."
            )
        }


    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    @DisplayName("Empty request")
    inner class EmptyRequest() {
        private lateinit var result: ResultActions

        @BeforeAll
        fun triggerEvent() {
            doNothing().`when`(javaMailSender).send(any(MimeMessage::class.java))

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
            doNothing().`when`(javaMailSender).send(any(MimeMessage::class.java))

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
            doNothing().`when`(javaMailSender).send(any(MimeMessage::class.java))

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
            doNothing().`when`(javaMailSender).send(any(MimeMessage::class.java))
            userRepository.save(User("existing@client.co", "password", UUID.randomUUID()))

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
            doNothing().`when`(javaMailSender).send(any(MimeMessage::class.java))

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