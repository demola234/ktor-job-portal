package com.example.models

import kotlinx.serialization.Serializable


@Serializable
enum class JobType { FULL_TIME, PART_TIME, CONTRACT, INTERNSHIP, REMOTE }

@Serializable
enum class JobStatus { OPEN, CLOSED }

@Serializable
data class Job(
    val id: String,
    val title: String,
    val company: String,
    val location: String,
    val type: JobType,
    val description: String,
    val requirements: List<String>,
    val salaryMin: Int? = null,
    val salaryMax: Int? = null,
    val status: JobStatus = JobStatus.OPEN,
    val postedAt: String,
    val postedBy: String
)

@Serializable
data class CreateJobRequest(
    val title: String,
    val company: String,
    val location: String,
    val type: JobType,
    val description: String,
    val requirements: List<String>,
    val salaryMin: Int? = null,
    val salaryMax: Int? = null
)

@Serializable
data class UpdateJobRequest(
    val title: String? = null,
    val company: String? = null,
    val location: String? = null,
    val type: JobType? = null,
    val description: String? = null,
    val requirements: List<String>? = null,
    val salaryMin: Int? = null,
    val salaryMax: Int? = null,
    val status: JobStatus? = null
)

@Serializable
enum class ApplicationStatus { PENDING, REVIEWED, SHORTLISTED, REJECTED, HIRED }

@Serializable
data class JobApplication(
    val id: String,
    val jobId: String,
    val applicantId: String,
    val applicantName: String,
    val applicantEmail: String,
    val coverLetter: String,
    val resumeUrl: String? = null,
    val status: ApplicationStatus = ApplicationStatus.PENDING,
    val appliedAt: String
)

@Serializable
data class CreateApplicationRequest(
    val jobId: String,
    val coverLetter: String,
    val resumeUrl: String? = null
)

@Serializable
data class UpdateApplicationStatusRequest(
    val status: ApplicationStatus
)

@Serializable
enum class UserRole { EMPLOYER, APPLICANT }

@Serializable
data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val passwordHash: String
)

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: UserRole
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val userId: String,
    val name: String,
    val role: UserRole
)

@Serializable
data class UserResponse(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole
)

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null
)

@Serializable
data class PaginatedResponse<T>(
    val items: List<T>,
    val total: Int,
    val page: Int,
    val pageSize: Int
)
