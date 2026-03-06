package com.mslyenglish

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.mslyenglish.config.toAppConfig
import com.mslyenglish.db.DatabaseFactory
import com.mslyenglish.models.ApiMessage
import com.mslyenglish.routes.authRoutes
import com.mslyenglish.routes.submissionRoutes
import com.mslyenglish.services.AuthService
import com.mslyenglish.services.SubmissionService
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

fun Application.module() {
    val appConfig = environment.config.toAppConfig()
    DatabaseFactory.init(appConfig.db)

    val authService = AuthService(appConfig.jwt)
    authService.ensureDefaultAdmin(appConfig.defaultAdmin.username, appConfig.defaultAdmin.password)
    val submissionService = SubmissionService(appConfig.storage.uploadDir)
    val logger = LoggerFactory.getLogger("Application")

    install(CallLogging)
    install(ContentNegotiation) { json() }
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Patch)
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error("Unhandled application error", cause)
            call.respond(HttpStatusCode.InternalServerError, ApiMessage("Internal server error"))
        }
    }
    install(Authentication) {
        jwt("auth-jwt") {
            verifier(
                JWT.require(Algorithm.HMAC256(appConfig.jwt.secret))
                    .withAudience(appConfig.jwt.audience)
                    .withIssuer(appConfig.jwt.issuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("userId").asLong() != null) JWTPrincipal(credential.payload) else null
            }
        }
    }

    routing {
        get("/health") { call.respond(ApiMessage("ok")) }
        authRoutes(authService)
        submissionRoutes(submissionService)
    }
}
