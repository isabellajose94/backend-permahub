package org.indie.isabella.permahub.controller.pub.user

import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.impl.DefaultClock
import io.jsonwebtoken.impl.DefaultJwtParser
import org.assertj.core.api.Assertions
import org.hamcrest.CoreMatchers
import org.indie.isabella.permahub.config.MongoInitializer
import org.indie.isabella.permahub.entity.User
import org.indie.isabella.permahub.entity.repository.UserRepository
import org.indie.isabella.permahub.model.http.request.ReAuthenticateData
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = [MongoInitializer::class])
@ActiveProfiles(value = ["test"])
@DisplayName("Re-authenticate Public User Controller Test")
class ReAuthenticateUserControllerTest {

    @SpyBean
    private lateinit var mockDefaultClock: DefaultClock

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Value("\${permahub.public.frontend.url}")
    private lateinit var PUBLIC_FRONT_END_URL: String

    @Value("\${jwt.access.secret}")
    private lateinit var JWT_ACCESS_SECRET: String

    @Value("\${jwt.refresh.secret}")
    private lateinit var JWT_REFRESH_SECRET: String

    private val objectMapper: ObjectMapper = ObjectMapper()

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    @DisplayName("Invalid token")
    inner class InvalidToken {
        private lateinit var result: ResultActions

        @BeforeAll
        fun triggerEvent() {
            result = mockMvc.perform(
                MockMvcRequestBuilders
                    .post("/public/api/users/re-authenticate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ReAuthenticateData(
                                "invalid token"
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
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.type", CoreMatchers.`is`("MalformedJwtException")))
                .andExpect(
                    MockMvcResultMatchers.jsonPath(
                        "$.error.message",
                        CoreMatchers.`is`("Invalid token")
                    )
                )
        }
    }
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    @DisplayName("Expired token")
    inner class ExpiredToken {
        private lateinit var result: ResultActions

        @BeforeAll
        fun triggerEvent() {
            val mockedDateTimeValue = LocalDateTime.of(2020, 2, 4, 7, 0)
            Mockito.`when`(mockDefaultClock.now())
                .thenReturn(Date.from(mockedDateTimeValue.atZone(ZoneId.systemDefault()).toInstant()))
            val refreshToken= Jwts.builder().setSubject("mail@mail.co").setIssuedAt(Date.from(LocalDateTime.of(2020, 2, 3, 2, 0).atZone(ZoneId.systemDefault()).toInstant()))
                .setExpiration(Date.from(LocalDateTime.of(2020, 2, 4, 2, 0).atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(SignatureAlgorithm.HS384, JWT_REFRESH_SECRET).compact()

            result = mockMvc.perform(
                MockMvcRequestBuilders
                    .post("/public/api/users/re-authenticate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ReAuthenticateData(refreshToken)
                        )
                    )
                    .header("Origin", PUBLIC_FRONT_END_URL)
            )
        }

        @Test
        fun shouldReturn401() {
            result
                .andExpect(MockMvcResultMatchers.status().isUnauthorized)
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.type", CoreMatchers.`is`("ExpiredJwtException")))
                .andExpect(
                    MockMvcResultMatchers.jsonPath(
                        "$.error.message",
                        CoreMatchers.`is`("Invalid token")
                    )
                )
        }
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    @DisplayName("Non existent email")
    inner class NonExistentEmail {
        private lateinit var result: ResultActions

        @BeforeAll
        fun triggerEvent() {
            val mockedDateTimeValue = LocalDateTime.of(2020, 2, 3, 7, 0)

            Mockito.`when`(mockDefaultClock.now())
                .thenReturn(Date.from(mockedDateTimeValue.atZone(ZoneId.systemDefault()).toInstant()))
            val refreshToken= Jwts.builder().setSubject("mail@mail.co").setIssuedAt(Date.from(LocalDateTime.of(2020, 2, 3, 2, 0).atZone(ZoneId.systemDefault()).toInstant()))
                .setExpiration(Date.from(LocalDateTime.of(2020, 2, 4, 2, 0).atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(SignatureAlgorithm.HS384, JWT_REFRESH_SECRET).compact()

            result = mockMvc.perform(
                MockMvcRequestBuilders
                    .post("/public/api/users/re-authenticate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ReAuthenticateData(refreshToken)
                        )
                    )
                    .header("Origin", PUBLIC_FRONT_END_URL)
            )
        }

        @Test
        fun shouldReturn401() {
            result
                .andExpect(MockMvcResultMatchers.status().isUnauthorized)
                .andExpect(MockMvcResultMatchers.jsonPath("$.error.type", CoreMatchers.`is`("UsernameNotFoundException")))
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
    @DisplayName("Existent email and valid token")
    inner class ExistentEmailAndValid {
        private lateinit var result: ResultActions

        val jwtAccessParser = Jwts.parser().setSigningKey(JWT_ACCESS_SECRET)
        val jwtRefreshParser = Jwts.parser().setSigningKey(JWT_REFRESH_SECRET)

        @BeforeAll
        fun triggerEvent() {
            val mockedDateTimeValue = LocalDateTime.of(2020, 2, 3, 7, 0)

            Mockito.`when`(mockDefaultClock.now())
                .thenReturn(Date.from(mockedDateTimeValue.atZone(ZoneId.systemDefault()).toInstant()))
            val refreshToken= Jwts.builder().setSubject("mail@mail.co").setIssuedAt(Date.from(LocalDateTime.of(2020, 2, 3, 2, 0).atZone(ZoneId.systemDefault()).toInstant()))
                .setExpiration(Date.from(LocalDateTime.of(2020, 2, 4, 2, 0).atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(SignatureAlgorithm.HS384, JWT_REFRESH_SECRET).compact()

            jwtAccessParser.setClock(mockDefaultClock)
            jwtRefreshParser.setClock(mockDefaultClock)

            val user = User("mail@mail.co", BCryptPasswordEncoder(16).encode("password"), UUID.randomUUID())
            user.verified = true
            userRepository.save(user)

            result = mockMvc.perform(
                MockMvcRequestBuilders
                    .post("/public/api/users/re-authenticate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ReAuthenticateData(refreshToken)
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
                .andExpect(
                    MockMvcResultMatchers.jsonPath(
                        "$.success.accessToken",
                        CoreMatchers.any(String::class.java)
                    )
                )
                .andExpect(
                    MockMvcResultMatchers.jsonPath(
                        "$.success.refreshToken",
                        CoreMatchers.any(String::class.java)
                    )
                )
                .andExpect(MockMvcResultMatchers.jsonPath("$.success.expiredAt", CoreMatchers.any(String::class.java)))

            val response = JSONObject(result.andReturn().response.contentAsString).getJSONObject("success")
            val accessToken = jwtAccessParser.parseClaimsJws(response["accessToken"].toString()).body
            val refreshToken = jwtRefreshParser.parseClaimsJws(response["refreshToken"].toString()).body

            Assertions.assertThat(accessToken.subject).isEqualTo("mail@mail.co")
            Assertions.assertThat(accessToken.issuedAt)
                .isEqualTo(Date.from(LocalDateTime.of(2020, 2, 3, 7, 0).atZone(ZoneId.systemDefault()).toInstant()))
            Assertions.assertThat(accessToken.expiration)
                .isEqualTo(Date.from(LocalDateTime.of(2020, 2, 3, 9, 0).atZone(ZoneId.systemDefault()).toInstant()))

            Assertions.assertThat(refreshToken.subject).isEqualTo("mail@mail.co")
            Assertions.assertThat(refreshToken.issuedAt)
                .isEqualTo(Date.from(LocalDateTime.of(2020, 2, 3, 7, 0).atZone(ZoneId.systemDefault()).toInstant()))
            Assertions.assertThat(refreshToken.expiration)
                .isEqualTo(Date.from(LocalDateTime.of(2020, 2, 4, 7, 0).atZone(ZoneId.systemDefault()).toInstant()))
            Assertions.assertThat(response["expiredAt"]).isEqualTo(
                LocalDateTime.of(2020, 2, 3, 9, 0).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_INSTANT)
            )
        }
    }
}