package com.example

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.database.DatabaseFactory
import com.example.models.ApiResponse
import com.example.repository.ApplicationRepository
import com.example.repository.JobRepository
import com.example.repository.UserRepository
import com.example.routes.applicationRoutes
import com.example.routes.authRoutes
import com.example.routes.jobRoutes
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.slf4j.event.Level


fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val jwtSecret = environment.config.property("jwt.secret").getString()

    DatabaseFactory.init(environment.config)

    val userRepository = UserRepository()
    val jobRepository = JobRepository()
    val applicationRepository = ApplicationRepository()

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    install(CallLogging) {
        level = Level.INFO
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled error", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiResponse<Unit>(false, message = "Internal server error: ${cause.message}")
            )
        }
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(status, ApiResponse<Unit>(false, message = "Resource not found"))
        }
        status(HttpStatusCode.Unauthorized) { call, status ->
            call.respond(status, ApiResponse<Unit>(false, message = "Authentication required"))
        }
    }

    install(Authentication) {
        jwt("jwt") {
            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret)).build()
            )
            validate { credential ->
                if (credential.payload.getClaim("id").asString() != null) JWTPrincipal(credential.payload) else null
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Unit>(false, message = "Invalid or expired token")
                )
            }
        }
    }

    routing {
        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok", "service" to "job-portal"))
        }

        authRoutes(userRepository, jwtSecret)
        jobRoutes(jobRepository, applicationRepository)
        applicationRoutes(userRepository, jobRepository, applicationRepository)
    }
}
