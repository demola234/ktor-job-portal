package com.example.routes

import com.example.models.CreateApplicationRequest
import com.example.repository.ApplicationRepository
import com.example.repository.JobRepository
import com.example.repository.UserRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.applicationRoutes(
    userRepository: UserRepository,
    jobRepository: JobRepository,
    applicationRepository: ApplicationRepository
) {
    authenticate("jwt") {
        route("/applications") {
            post {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.getClaim("id", String::class)
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val request = call.receive<CreateApplicationRequest>()
                val job = jobRepository.findById(request.jobId)
                    ?: return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Job not found"))
                val user = userRepository.findById(userId)
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val application = applicationRepository.create(request.jobId, request, user)
                call.respond(HttpStatusCode.Created, application)
            }

            get {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.getClaim("id", String::class)
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val applications = applicationRepository.findByApplicant(userId)
                call.respond(applications)
            }
        }

        post("/jobs/{id}/apply") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.getClaim("id", String::class)
                ?: return@post call.respond(HttpStatusCode.Unauthorized)
            val jobId = call.parameters["id"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing job ID"))
            val job = jobRepository.findById(jobId)
                ?: return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Job not found"))
            val user = userRepository.findById(userId)
                ?: return@post call.respond(HttpStatusCode.Unauthorized)
            val request = call.receive<CreateApplicationRequest>()
            val application = applicationRepository.create(jobId, request, user)
            call.respond(HttpStatusCode.Created, application)
        }
    }
}
