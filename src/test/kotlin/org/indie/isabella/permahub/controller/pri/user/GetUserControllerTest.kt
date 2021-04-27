package org.indie.isabella.permahub.controller.pri.user

import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.CoreMatchers
import org.indie.isabella.permahub.config.MongoInitializer
import org.indie.isabella.permahub.entity.User
import org.indie.isabella.permahub.entity.repository.UserRepository
import org.indie.isabella.permahub.model.http.request.UserData
import org.json.JSONObject
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
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = [MongoInitializer::class])
@ActiveProfiles(value = ["test"])
@DisplayName("Get Private User Controller Test")
class GetUserControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    private val objectMapper: ObjectMapper = ObjectMapper()

    @Value("\${permahub.public.frontend.url}")
    private lateinit var PUBLIC_FRONT_END_URL: String

    @Value("\${permahub.private.frontend.url}")
    private lateinit var PRIVATE_FRONT_END_URL: String

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    @DisplayName("Non existent JWT")
    inner class NonExistentJWT() {
        private lateinit var result: ResultActions

        @BeforeAll
        fun triggerEvent() {
            result = mockMvc.perform(
                    MockMvcRequestBuilders
                            .get("/api/users")
            )
        }

        @Test
        fun shouldReturn401() {
            result
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized)
                    .andExpect(
                            MockMvcResultMatchers.jsonPath(
                                    "$.error.type",
                                    CoreMatchers.`is`("InsufficientAuthenticationException")
                            )
                    )
                    .andExpect(
                            MockMvcResultMatchers.jsonPath(
                                    "$.error.message",
                                    CoreMatchers.`is`("Full authentication is required")
                            )
                    )
        }
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    @DisplayName("Wrong JWT")
    inner class WrongJWT() {
        private lateinit var result: ResultActions

        @BeforeAll
        fun triggerEvent() {
            result = mockMvc.perform(
                    MockMvcRequestBuilders
                            .get("/api/users")
                            .header("Authorization", "Bearer ssssss")
            )
        }

        @Test
        fun shouldReturn401() {
            result
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized)
                    .andExpect(
                            MockMvcResultMatchers.jsonPath(
                                    "$.error.type",
                                    CoreMatchers.`is`("InsufficientAuthenticationException")
                            )
                    )
                    .andExpect(
                            MockMvcResultMatchers.jsonPath(
                                    "$.error.message",
                                    CoreMatchers.`is`("Full authentication is required")
                            )
                    )
        }
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    @DisplayName("Unauthorize CORS origin")
    inner class UnauthorizeCORSOrigin() {
        private lateinit var result: ResultActions

        @BeforeAll
        fun triggerEvent() {
            val user = User(
                    "existing@client.co",
                    BCryptPasswordEncoder(16).encode("password"), UUID.randomUUID())
            user.verified = true
            userRepository.save(user)
            val authResult = mockMvc.perform(
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


            val jwt = JSONObject(authResult.andReturn().response.contentAsString).getJSONObject("success")["accessToken"]

            result = mockMvc.perform(
                    MockMvcRequestBuilders
                            .get("/api/users")
                            .header("Origin", "Invalid cors")
                            .header("Authorization", "Bearer $jwt")
            )
        }

        @AfterAll
        fun clearDB() {
            userRepository.deleteAll()
        }


        @Test
        fun shouldReturn403() {
            result
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(MockMvcResultMatchers.status().isForbidden)
                    .andExpect(
                            MockMvcResultMatchers.jsonPath(
                                    "$.error.type",
                                    CoreMatchers.`is`("InvalidCORS")
                            )
                    )
                    .andExpect(
                            MockMvcResultMatchers.jsonPath(
                                    "$.error.message",
                                    CoreMatchers.`is`("Invalid CORS request")
                            )
                    )
        }
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    @DisplayName("Authorize CORS origin and valid JWT")
    inner class AuthorizeCORSOriginAndValidJWT() {
        private lateinit var result: ResultActions

        @BeforeAll
        fun triggerEvent() {
            val user = User(
                    "existing@client.co",
                    BCryptPasswordEncoder(16).encode("password"), UUID.randomUUID())
            user.verified = true
            userRepository.save(user)
            val authResult = mockMvc.perform(
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


            val jwt = JSONObject(authResult.andReturn().response.contentAsString).getJSONObject("success")["accessToken"]

            result = mockMvc.perform(
                    MockMvcRequestBuilders
                            .get("/api/users")
                            .header("Origin", PRIVATE_FRONT_END_URL)
                            .header("Authorization", "Bearer $jwt")
            )
        }

        @AfterAll
        fun clearDB() {
            userRepository.deleteAll()
        }


        @Test
        fun shouldReturn200() {
            result
                    .andExpect(MockMvcResultMatchers.status().isOk)
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success.email", CoreMatchers.`is`("existing@client.co")))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success.lastModifiedDate").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success.createdDate").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success.password").doesNotExist())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success.id").doesNotExist())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success.verificationCode").doesNotExist())
        }
    }
}