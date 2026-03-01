package com.example.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.models.*
import com.example.repository.UserRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.Date

fun Route.authRoutes(userRepository: UserRepository, jwtSecret: String) {
    route("/auth") {
        post("/register") {
            val request = call.receive<RegisterRequest>()
            if (userRepository.emailExists(request.email)) {
                return@post call.respond(HttpStatusCode.Conflict, mapOf("error" to "Email already in use"))
            }
            val user = userRepository.create(request, hashPassword(request.password))
            call.respond(HttpStatusCode.Created, UserResponse(user.id, user.name, user.email, user.role))
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            val user = userRepository.findByEmail(request.email)
            if (user == null || user.passwordHash != hashPassword(request.password)) {
                return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid email or password"))
            }
            val token = JWT.create()
                .withClaim("id", user.id)
                .withClaim("role", user.role.name)
                .withExpiresAt(Date(System.currentTimeMillis() + 86_400_000L))
                .sign(Algorithm.HMAC256(jwtSecret))
            call.respond(HttpStatusCode.OK, AuthResponse(token, user.id, user.name, user.role))
        }
    }
}

private fun hashPassword(password: String): String {
    val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}
