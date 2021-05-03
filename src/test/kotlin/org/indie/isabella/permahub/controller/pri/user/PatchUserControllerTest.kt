package org.indie.isabella.permahub.controller.pri.user

import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.impl.DefaultClock
import org.assertj.core.api.Assertions
import org.hamcrest.CoreMatchers.*
import org.indie.isabella.permahub.config.MongoInitializer
import org.indie.isabella.permahub.entity.User
import org.indie.isabella.permahub.entity.repository.UserRepository
import org.indie.isabella.permahub.model.Area
import org.indie.isabella.permahub.model.Contact
import org.indie.isabella.permahub.model.http.request.UserData
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.*
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.MediaType
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.awt.print.Book
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*


@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = [MongoInitializer::class])
@ActiveProfiles(value = ["test"])
@DisplayName("Patch Private User Controller Test")
class PatchUserControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @SpyBean
    private lateinit var mockDefaultClock: DefaultClock

    private val objectMapper: ObjectMapper = ObjectMapper()

    @Value("\${permahub.private.frontend.url}")
    private lateinit var PRIVATE_FRONT_END_URL: String

    @Value("\${permahub.public.frontend.url}")
    private lateinit var PUBLIC_FRONT_END_URL: String

    @Value("\${jwt.access.secret}")
    private lateinit var JWT_ACCESS_SECRET: String

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    @DisplayName("Non existent JWT")
    inner class NonExistentJWT() {
        private lateinit var result: ResultActions

        @BeforeAll
        fun triggerEvent() {
            result = mockMvc.perform(
                MockMvcRequestBuilders
                    .patch("/api/users")
            )
        }

        @Test
        fun shouldReturn401() {
            result
                .andExpect(status().isUnauthorized)
                .andExpect(
                    jsonPath(
                        "$.error.type",
                        `is`("InsufficientAuthenticationException")
                    )
                )
                .andExpect(
                    jsonPath(
                        "$.error.message",
                        `is`("Full authentication is required")
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
                    .patch("/api/users")
                    .header("Authorization", "Bearer ssssss")
            )
        }

        @Test
        fun shouldReturn401() {
            result
                .andExpect(status().isUnauthorized)
                .andExpect(
                    jsonPath(
                        "$.error.type",
                        `is`("InsufficientAuthenticationException")
                    )
                )
                .andExpect(
                    jsonPath(
                        "$.error.message",
                        `is`("Full authentication is required")
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
                BCryptPasswordEncoder(16).encode("password"), UUID.randomUUID()
            )
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

            val jwt =
                JSONObject(authResult.andReturn().response.contentAsString).getJSONObject("success")["accessToken"]

            result = mockMvc.perform(
                MockMvcRequestBuilders
                    .patch("/api/users")
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
                .andExpect(status().isForbidden)
                .andExpect(
                    jsonPath(
                        "$.error.type",
                        `is`("InvalidCORS")
                    )
                )
                .andExpect(
                    jsonPath(
                        "$.error.message",
                        `is`("Invalid CORS request")
                    )
                )
        }
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    @DisplayName("Authorize CORS origin and valid JWT but user doesn't exist")
    inner class AuthorizeCORSOriginAndValidJWTButUserDoesntExist() {
        private lateinit var result: ResultActions

        @BeforeAll
        fun triggerEvent() {
            val mockedDateTimeValue = LocalDateTime.of(2020, 2, 3, 7, 0)

            Mockito.`when`(mockDefaultClock.now())
                .thenReturn(Date.from(mockedDateTimeValue.atZone(ZoneId.systemDefault()).toInstant()))
            val accessToken = Jwts.builder().setSubject("mail@mail.co").setIssuedAt(
                Date.from(
                    LocalDateTime.of(2020, 2, 3, 6, 0).atZone(
                        ZoneId.systemDefault()
                    ).toInstant()
                )
            )
                .setExpiration(Date.from(LocalDateTime.of(2020, 2, 3, 8, 0).atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(SignatureAlgorithm.HS512, JWT_ACCESS_SECRET).compact()

            result = mockMvc.perform(
                MockMvcRequestBuilders
                    .patch("/api/users")
                    .header("Origin", PRIVATE_FRONT_END_URL)
                    .header("Authorization", "Bearer $accessToken")

            )
        }

        @AfterAll
        fun clearDB() {
            userRepository.deleteAll()
        }


        @Test
        fun shouldReturn401() {
            result
                .andExpect(status().isUnauthorized)
                .andExpect(
                    jsonPath(
                        "$.error.type",
                        `is`("InsufficientAuthenticationException")
                    )
                )
                .andExpect(
                    jsonPath(
                        "$.error.message",
                        `is`("Full authentication is required")
                    )
                )
        }
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    @DisplayName("Authorize CORS origin and valid JWT but request body empty")
    inner class AuthorizeCORSOriginAndValidJWTButRequestBodyEmpty() {
        private lateinit var result: ResultActions

        @BeforeAll
        fun triggerEvent() {
            val mockedDateTimeValue = LocalDateTime.of(2020, 2, 3, 7, 0)

            val user = User("mail@mail.co", BCryptPasswordEncoder(16).encode("password"), UUID.randomUUID())
            user.verified = true
            userRepository.save(user)

            Mockito.`when`(mockDefaultClock.now())
                .thenReturn(Date.from(mockedDateTimeValue.atZone(ZoneId.systemDefault()).toInstant()))
            val accessToken = Jwts.builder().setSubject("mail@mail.co").setIssuedAt(
                Date.from(
                    LocalDateTime.of(2020, 2, 3, 6, 0).atZone(
                        ZoneId.systemDefault()
                    ).toInstant()
                )
            )
                .setExpiration(Date.from(LocalDateTime.of(2020, 2, 3, 8, 0).atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(SignatureAlgorithm.HS512, JWT_ACCESS_SECRET).compact()

            result = mockMvc.perform(
                MockMvcRequestBuilders
                    .patch("/api/users")
                    .header("Origin", PRIVATE_FRONT_END_URL)
                    .header("Authorization", "Bearer $accessToken")
            )
        }

        @AfterAll
        fun clearDB() {
            userRepository.deleteAll()
        }


        @Test
        fun shouldReturn400() {
            result
                .andExpect(status().isBadRequest)
                .andExpect(
                    jsonPath(
                        "$.error.type",
                        `is`("HttpMessageNotReadableException")
                    )
                )
                .andExpect(
                    jsonPath(
                        "$.error.message",
                        `is`("Required request body is missing")
                    )
                )
        }
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    @DisplayName("Authorize CORS origin and valid JWT with all fields")
    inner class AuthorizeCORSOriginAndValidJWTWithAllFields() {
        private lateinit var result: ResultActions

        @BeforeAll
        fun triggerEvent() {
            val mockedDateTimeValue = LocalDateTime.of(2020, 2, 3, 7, 0)

            val user = User("mail@mail.co", BCryptPasswordEncoder(16).encode("password"), UUID.randomUUID())
            user.verified = true
            userRepository.save(user)

            Mockito.`when`(mockDefaultClock.now())
                .thenReturn(Date.from(mockedDateTimeValue.atZone(ZoneId.systemDefault()).toInstant()))
            val accessToken = Jwts.builder().setSubject("mail@mail.co").setIssuedAt(
                Date.from(
                    LocalDateTime.of(2020, 2, 3, 6, 0).atZone(
                        ZoneId.systemDefault()
                    ).toInstant()
                )
            )
                .setExpiration(Date.from(LocalDateTime.of(2020, 2, 3, 8, 0).atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(SignatureAlgorithm.HS512, JWT_ACCESS_SECRET).compact()

            result = mockMvc.perform(
                MockMvcRequestBuilders
                    .patch("/api/users")
                    .header("Origin", PRIVATE_FRONT_END_URL)
                    .header("Authorization", "Bearer $accessToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\n" +
                                "  \"name\": \"Mr. Sandman\",\n" +
                                "  \"headline\": \"I'm Mr. Sandman and I like sands.\",\n" +
                                "  \"about\": \"I'm a date farmer, that's why I like sands.\",\n" +
                                "  \"type\": \"farmer\",\n" +
                                "  \"area\": {\n" +
                                "    \"country\": \"MA\",\n" +
                                "    \"region\": \"MA-08\"\n" +
                                "  },\n" +
                                "  \"contact\": {\n" +
                                "    \"private\": \"My Telegram: +212 1234 1234\",\n" +
                                "    \"public\": \"My e-mail: mrsandman@notreal.co\"\n" +
                                "  }\n" +
                                "}"
                    )
            )
            Locale.ITALY.displayCountry
        }

        @AfterAll
        fun clearDB() {
            userRepository.deleteAll()
        }
        @Test
        fun shouldSaveUser() {
            val users = userRepository.findAll()
            Assertions.assertThat(users).hasSize(1)
            Assertions.assertThat(users[0].email).isEqualTo("mail@mail.co")
            Assertions.assertThat(users[0].name).isEqualTo("Mr. Sandman")
            Assertions.assertThat(users[0].headline).isEqualTo("I'm Mr. Sandman and I like sands.")
            Assertions.assertThat(users[0].about).isEqualTo("I'm a date farmer, that's why I like sands.")
            Assertions.assertThat(users[0].type).isEqualTo("farmer")
            Assertions.assertThat(users[0].area?.country).isEqualTo("MA")
            Assertions.assertThat(users[0].area?.region).isEqualTo("MA-08")
            Assertions.assertThat(users[0].contact?.private).isEqualTo("My Telegram: +212 1234 1234")
            Assertions.assertThat(users[0].contact?.public).isEqualTo("My e-mail: mrsandman@notreal.co")
        }

        @Test
        fun shouldReturn200() {
            result
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success.email", `is`("mail@mail.co")))
                .andExpect(jsonPath("$.success.name", `is`("Mr. Sandman")))
                .andExpect(jsonPath("$.success.headline", `is`("I'm Mr. Sandman and I like sands.")))
                .andExpect(jsonPath("$.success.about", `is`("I'm a date farmer, that's why I like sands.")))
                .andExpect(jsonPath("$.success.type", `is`("farmer")))
                .andExpect(jsonPath("$.success.area.country", `is`("MA")))
                .andExpect(jsonPath("$.success.area.region", `is`("MA-08")))
                .andExpect(jsonPath("$.success.contact.private", `is`("My Telegram: +212 1234 1234")))
                .andExpect(jsonPath("$.success.contact.public", `is`("My e-mail: mrsandman@notreal.co")))
                .andExpect(jsonPath("$.success.password").doesNotExist())
                .andExpect(jsonPath("$.success.id").doesNotExist())
        }
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    @DisplayName("Authorize CORS origin and valid JWT with some fields")
    inner class AuthorizeCORSOriginAndValidJWTWitSomeFields() {
        private lateinit var result: ResultActions

        @BeforeAll
        fun triggerEvent() {
            val mockedDateTimeValue = LocalDateTime.of(2020, 2, 3, 7, 0)

            val user = User("mail@mail.co", BCryptPasswordEncoder(16).encode("password"), UUID.randomUUID())
            user.verified = true
            user.name = "unchanged"
            user.headline = "unchanged"
            user.about = "unchanged"
            user.type = "unchanged"
            user.area = Area("MA", "MA-08")
            user.contact = Contact("unchanged", "unchanged")
            userRepository.save(user)

            Mockito.`when`(mockDefaultClock.now())
                .thenReturn(Date.from(mockedDateTimeValue.atZone(ZoneId.systemDefault()).toInstant()))
            val accessToken = Jwts.builder().setSubject("mail@mail.co").setIssuedAt(
                Date.from(
                    LocalDateTime.of(2020, 2, 3, 6, 0).atZone(
                        ZoneId.systemDefault()
                    ).toInstant()
                )
            )
                .setExpiration(Date.from(LocalDateTime.of(2020, 2, 3, 8, 0).atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(SignatureAlgorithm.HS512, JWT_ACCESS_SECRET).compact()

            result = mockMvc.perform(
                MockMvcRequestBuilders
                    .patch("/api/users")
                    .header("Origin", PRIVATE_FRONT_END_URL)
                    .header("Authorization", "Bearer $accessToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\n" +
                                "  \"name\": \"Mr. Sandman\",\n" +

                                "  \"type\": \"farmer\",\n" +
                                "  \"area\": {\n" +
                                "    \"country\": \"ID\",\n" +
                                "    \"region\": \"JI\"\n" +
                                "  }\n" +
                                "}"
                    )
            )
            Locale.ITALY.displayCountry
        }

        @AfterAll
        fun clearDB() {
            userRepository.deleteAll()
        }
        @Test
        fun shouldSaveUser() {
            val users = userRepository.findAll()
            Assertions.assertThat(users).hasSize(1)
            Assertions.assertThat(users[0].email).isEqualTo("mail@mail.co")
            Assertions.assertThat(users[0].name).isEqualTo("Mr. Sandman")
            Assertions.assertThat(users[0].headline).isEqualTo("unchanged")
            Assertions.assertThat(users[0].about).isEqualTo("unchanged")
            Assertions.assertThat(users[0].type).isEqualTo("farmer")
            Assertions.assertThat(users[0].area?.country).isEqualTo("ID")
            Assertions.assertThat(users[0].area?.region).isEqualTo("JI")
            Assertions.assertThat(users[0].contact?.private).isEqualTo("unchanged")
            Assertions.assertThat(users[0].contact?.public).isEqualTo("unchanged")
        }

        @Test
        fun shouldReturn200() {
            result
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success.email", `is`("mail@mail.co")))
                .andExpect(jsonPath("$.success.name", `is`("Mr. Sandman")))
                .andExpect(jsonPath("$.success.headline", `is`("unchanged")))
                .andExpect(jsonPath("$.success.about", `is`("unchanged")))
                .andExpect(jsonPath("$.success.type", `is`("farmer")))
                .andExpect(jsonPath("$.success.area.country", `is`("ID")))
                .andExpect(jsonPath("$.success.area.region", `is`("JI")))
                .andExpect(jsonPath("$.success.contact.private", `is`("unchanged")))
                .andExpect(jsonPath("$.success.contact.public", `is`("unchanged")))
                .andExpect(jsonPath("$.success.password").doesNotExist())
                .andExpect(jsonPath("$.success.id").doesNotExist())
        }
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    @DisplayName("Authorize CORS origin and valid JWT with invalid country")
    inner class AuthorizeCORSOriginAndValidJWTWitInvalidCountry() {
        private lateinit var result: ResultActions

        @BeforeAll
        fun triggerEvent() {
            val mockedDateTimeValue = LocalDateTime.of(2020, 2, 3, 7, 0)

            val user = User("mail@mail.co", BCryptPasswordEncoder(16).encode("password"), UUID.randomUUID())
            user.verified = true
            user.name = "unchanged"
            user.headline = "unchanged"
            user.about = "unchanged"
            user.type = "unchanged"
            user.area = Area("MA", "MA-08")
            user.contact = Contact("unchanged", "unchanged")
            userRepository.save(user)

            Mockito.`when`(mockDefaultClock.now())
                .thenReturn(Date.from(mockedDateTimeValue.atZone(ZoneId.systemDefault()).toInstant()))
            val accessToken = Jwts.builder().setSubject("mail@mail.co").setIssuedAt(
                Date.from(
                    LocalDateTime.of(2020, 2, 3, 6, 0).atZone(
                        ZoneId.systemDefault()
                    ).toInstant()
                )
            )
                .setExpiration(Date.from(LocalDateTime.of(2020, 2, 3, 8, 0).atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(SignatureAlgorithm.HS512, JWT_ACCESS_SECRET).compact()

            result = mockMvc.perform(
                MockMvcRequestBuilders
                    .patch("/api/users")
                    .header("Origin", PRIVATE_FRONT_END_URL)
                    .header("Authorization", "Bearer $accessToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\n" +
                                "  \"name\": \"Mr. Sandman\",\n" +

                                "  \"type\": \"farmer\",\n" +
                                "  \"area\": {\n" +
                                "    \"country\": \"XY\"\n" +
                                "  }\n" +
                                "}"
                    )
            )
            Locale.ITALY.displayCountry
        }

        @AfterAll
        fun clearDB() {
            userRepository.deleteAll()
        }
        @Test
        fun shouldNotSaveUser() {
            val users = userRepository.findAll()
            Assertions.assertThat(users).hasSize(1)
            Assertions.assertThat(users[0].email).isEqualTo("mail@mail.co")
            Assertions.assertThat(users[0].name).isEqualTo("unchanged")
            Assertions.assertThat(users[0].headline).isEqualTo("unchanged")
            Assertions.assertThat(users[0].about).isEqualTo("unchanged")
            Assertions.assertThat(users[0].type).isEqualTo("unchanged")
            Assertions.assertThat(users[0].area?.country).isEqualTo("MA")
            Assertions.assertThat(users[0].area?.region).isEqualTo("MA-08")
            Assertions.assertThat(users[0].contact?.private).isEqualTo("unchanged")
            Assertions.assertThat(users[0].contact?.public).isEqualTo("unchanged")
        }

        @Test
        fun shouldReturn400() {
            result
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.type", `is`("BadInputException")))
                .andExpect(jsonPath("$.error.message", `is`("'XY' is invalid country code, please refer to ISO3166-2")))
        }
    }
}