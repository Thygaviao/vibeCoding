package com.mslyenglish.routes

import com.mslyenglish.models.ApiMessage
import com.mslyenglish.models.LoginRequest
import com.mslyenglish.models.LoginResponse
import com.mslyenglish.services.AuthService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(authService: AuthService) {
    route("/api/admin/auth") {
        post("/login") {
            val body = call.receive<LoginRequest>()
            if (body.username.isBlank() || body.password.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiMessage("Username and password are required"))
                return@post
            }
            val token = authService.login(body.username.trim(), body.password)
            if (token == null) call.respond(HttpStatusCode.Unauthorized, ApiMessage("Invalid credentials"))
            else call.respond(LoginResponse(token))
        }

        post("/logout") { call.respond(ApiMessage("Logged out")) }

        authenticate("auth-jwt") {
            get("/me") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, ApiMessage("Unauthorized"))
                    return@get
                }
                val me = authService.me(userId)
                if (me == null) call.respond(HttpStatusCode.NotFound, ApiMessage("Admin not found")) else call.respond(me)
            }
        }
    }
}
