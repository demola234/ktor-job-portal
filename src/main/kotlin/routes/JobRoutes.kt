package com.example.routes

import com.example.models.*
import com.example.repository.ApplicationRepository
import com.example.repository.JobRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.jobRoutes(jobRepository: JobRepository, applicationRepository: ApplicationRepository) {
    route("/jobs") {
        get {
            val keyword = call.request.queryParameters["keyword"]
            val location = call.request.queryParameters["location"]
            val type = call.request.queryParameters["type"]
                ?.let { runCatching { JobType.valueOf(it.uppercase()) }.getOrNull() }
            val status = call.request.queryParameters["status"] ?: "OPEN"
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 10

            val result = jobRepository.findAll(keyword, type, location, JobStatus.valueOf(status), page, pageSize)
            call.respond(HttpStatusCode.OK, ApiResponse<PaginatedResponse<Job>>(success = true, data = result))
        }

        get("/{id}") {
            val id = call.parameters["id"]
            val job = jobRepository.findById(id ?: "")
            if (job != null) {
                call.respond(HttpStatusCode.OK, ApiResponse<Job>(success = true, data = job))
            } else {
                call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, message = "Job not found"))
            }
        }

        authenticate("jwt") {
            post {
                val principal = call.principal<JWTPrincipal>()
                requireRole(principal, UserRole.EMPLOYER) ?: return@post call.respond(
                    HttpStatusCode.Forbidden,
                    ApiResponse<Unit>(success = false, message = "Access denied")
                )

                val request = call.receive<CreateJobRequest>()
                val job = jobRepository.create(request, principal!!.getClaim("id", String::class)!!)
                call.respond(HttpStatusCode.Created, ApiResponse<Job>(success = true, data = job))
            }

            put("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                requireRole(principal, UserRole.EMPLOYER) ?: return@put call.respond(
                    HttpStatusCode.Forbidden,
                    ApiResponse<Unit>(success = false, message = "Access denied")
                )

                val id = call.parameters["id"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, message = "Missing job ID")
                )

                val existingJob = jobRepository.findById(id)
                if (existingJob == null) {
                    return@put call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, message = "Job not found"))
                }

                if (existingJob.postedBy != principal!!.getClaim("id", String::class)) {
                    return@put call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(success = false, message = "You can only update your own jobs"))
                }

                val request = call.receive<UpdateJobRequest>()
                val updatedJob = jobRepository.update(id, request)
                if (updatedJob != null) {
                    call.respond(HttpStatusCode.OK, ApiResponse<Job>(success = true, data = updatedJob))
                } else {
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, message = "Failed to update job"))
                }
            }

            delete("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                requireRole(principal, UserRole.EMPLOYER) ?: return@delete call.respond(
                    HttpStatusCode.Forbidden,
                    ApiResponse<Unit>(success = false, message = "Access denied")
                )

                val id = call.parameters["id"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, message = "Missing job ID")
                )

                val existingJob = jobRepository.findById(id)
                if (existingJob == null) {
                    return@delete call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, message = "Job not found"))
                }

                if (existingJob.postedBy != principal!!.getClaim("id", String::class)) {
                    return@delete call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(success = false, message = "You can only delete your own jobs"))
                }

                val deleted = jobRepository.delete(id)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, ApiResponse<Unit>(success = true, message = "Job deleted successfully"))
                } else {
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, message = "Failed to delete job"))
                }
            }

            get("/mine") {
                val principal = call.principal<JWTPrincipal>()
                requireRole(principal, UserRole.EMPLOYER) ?: return@get call.respond(
                    HttpStatusCode.Forbidden,
                    ApiResponse<Unit>(success = false, message = "Access denied")
                )

                val jobs = jobRepository.findByEmployer(principal!!.getClaim("id", String::class)!!)
                call.respond(HttpStatusCode.OK, ApiResponse<List<Job>>(success = true, data = jobs))
            }

            get("/{id}/applications") {
                val principal = call.principal<JWTPrincipal>()
                requireRole(principal, UserRole.EMPLOYER) ?: return@get call.respond(
                    HttpStatusCode.Forbidden,
                    ApiResponse<Unit>(success = false, message = "Access denied")
                )

                val id = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, message = "Missing job ID")
                )

                val job = jobRepository.findById(id)
                if (job == null) {
                    return@get call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, message = "Job not found"))
                }

                if (job.postedBy != principal!!.getClaim("id", String::class)) {
                    return@get call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(success = false, message = "You can only view applications for your own jobs"))
                }

                val applications = applicationRepository.findByJob(id)
                call.respond(HttpStatusCode.OK, ApiResponse<List<JobApplication>>(success = true, data = applications))
            }
        }
    }
}

fun requireRole(principal: JWTPrincipal?, role: UserRole): Unit? {
    if (principal == null) return null
    return if (principal.getClaim("role", String::class) == role.name) Unit else null
}
