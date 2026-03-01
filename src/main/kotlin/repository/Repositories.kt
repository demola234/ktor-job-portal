package com.example.repository

import com.example.models.Job
import com.example.database.*
import com.example.models.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.Instant
import java.util.UUID

// ── Helpers ───────────────────────────────────────────────────────────────────

private val json = Json { ignoreUnknownKeys = true }

private fun ResultRow.toJob() = Job(
    id           = this[JobsTable.id],
    title        = this[JobsTable.title],
    company      = this[JobsTable.company],
    location     = this[JobsTable.location],
    type         = JobType.valueOf(this[JobsTable.type]),
    description  = this[JobsTable.description],
    requirements = json.decodeFromString(this[JobsTable.requirements]),
    salaryMin    = this[JobsTable.salaryMin],
    salaryMax    = this[JobsTable.salaryMax],
    status       = JobStatus.valueOf(this[JobsTable.status]),
    postedBy     = this[JobsTable.postedBy],
    postedAt     = this[JobsTable.postedAt].toString()
)

private fun ResultRow.toApplication() = JobApplication(
    id             = this[ApplicationsTable.id],
    jobId          = this[ApplicationsTable.jobId],
    applicantId    = this[ApplicationsTable.applicantId],
    applicantName  = this[ApplicationsTable.applicantName],
    applicantEmail = this[ApplicationsTable.applicantEmail],
    coverLetter    = this[ApplicationsTable.coverLetter],
    resumeUrl      = this[ApplicationsTable.resumeUrl],
    status         = ApplicationStatus.valueOf(this[ApplicationsTable.status]),
    appliedAt      = this[ApplicationsTable.appliedAt].toString()
)

private fun ResultRow.toUser() = User(
    id           = this[UsersTable.id],
    name         = this[UsersTable.name],
    email        = this[UsersTable.email],
    role         = UserRole.valueOf(this[UsersTable.role]),
    passwordHash = this[UsersTable.passwordHash]
)

private fun now() = Instant.now()

// ── Job Repository ────────────────────────────────────────────────────────────

class JobRepository {

    suspend fun findAll(
        keyword: String? = null,
        type: JobType? = null,
        location: String? = null,
        status: JobStatus = JobStatus.OPEN,
        page: Int = 1,
        pageSize: Int = 10
    ): PaginatedResponse<Job> = dbQuery {
        val query = JobsTable.selectAll()
            .where { JobsTable.status eq status.name }
            .apply {
                if (keyword != null) {
                    andWhere {
                        (JobsTable.title.lowerCase() like "%${keyword.lowercase()}%") or
                                (JobsTable.description.lowerCase() like "%${keyword.lowercase()}%")
                    }
                }
                if (type != null)     andWhere { JobsTable.type eq type.name }
                if (location != null) andWhere { JobsTable.location.lowerCase() like "%${location.lowercase()}%" }
            }
            .orderBy(JobsTable.postedAt, SortOrder.DESC)

        val total = query.count().toInt()
        val items = query
            .limit(pageSize, offset = ((page - 1) * pageSize).toLong())
            .map { it.toJob() }

        PaginatedResponse(items, total, page, pageSize)
    }

    suspend fun findById(id: String): Job? = dbQuery {
        JobsTable.selectAll().where { JobsTable.id eq id }.singleOrNull()?.toJob()
    }

    suspend fun findByEmployer(employerId: String): List<Job> = dbQuery {
        JobsTable.selectAll()
            .where { JobsTable.postedBy eq employerId }
            .orderBy(JobsTable.postedAt, SortOrder.DESC)
            .map { it.toJob() }
    }

    suspend fun create(request: CreateJobRequest, employerId: String): Job = dbQuery {
        val id = UUID.randomUUID().toString()
        val postedAt = now()
        JobsTable.insert {
            it[JobsTable.id]           = id
            it[JobsTable.title]        = request.title
            it[JobsTable.company]      = request.company
            it[JobsTable.location]     = request.location
            it[JobsTable.type]         = request.type.name
            it[JobsTable.description]  = request.description
            it[JobsTable.requirements] = json.encodeToString(request.requirements)
            it[JobsTable.salaryMin]    = request.salaryMin
            it[JobsTable.salaryMax]    = request.salaryMax
            it[JobsTable.status]       = JobStatus.OPEN.name
            it[JobsTable.postedBy]     = employerId
            it[JobsTable.postedAt]     = postedAt
        }
        Job(
            id           = id,
            title        = request.title,
            company      = request.company,
            location     = request.location,
            type         = request.type,
            description  = request.description,
            requirements = request.requirements,
            salaryMin    = request.salaryMin,
            salaryMax    = request.salaryMax,
            status       = JobStatus.OPEN,
            postedBy     = employerId,
            postedAt     = postedAt.toString()
        )
    }

    suspend fun update(id: String, request: UpdateJobRequest): Job? = dbQuery {
        val existing = JobsTable.selectAll().where { JobsTable.id eq id }.singleOrNull()?.toJob()
            ?: return@dbQuery null
        JobsTable.update({ JobsTable.id eq id }) {
            it[JobsTable.title]        = request.title        ?: existing.title
            it[JobsTable.location]     = request.location     ?: existing.location
            it[JobsTable.type]         = (request.type        ?: existing.type).name
            it[JobsTable.description]  = request.description  ?: existing.description
            it[JobsTable.requirements] = json.encodeToString(request.requirements ?: existing.requirements)
            it[JobsTable.salaryMin]    = request.salaryMin    ?: existing.salaryMin
            it[JobsTable.salaryMax]    = request.salaryMax    ?: existing.salaryMax
            it[JobsTable.status]       = (request.status      ?: existing.status).name
        }
        JobsTable.selectAll().where { JobsTable.id eq id }.singleOrNull()?.toJob()
    }

    suspend fun delete(id: String): Boolean = dbQuery {
        JobsTable.deleteWhere { JobsTable.id eq id } > 0
    }
}

// ── Application Repository ────────────────────────────────────────────────────

class ApplicationRepository {

    suspend fun findByJob(jobId: String): List<JobApplication> = dbQuery {
        ApplicationsTable.selectAll()
            .where { ApplicationsTable.jobId eq jobId }
            .orderBy(ApplicationsTable.appliedAt, SortOrder.DESC)
            .map { it.toApplication() }
    }

    suspend fun findByApplicant(applicantId: String): List<JobApplication> = dbQuery {
        ApplicationsTable.selectAll()
            .where { ApplicationsTable.applicantId eq applicantId }
            .orderBy(ApplicationsTable.appliedAt, SortOrder.DESC)
            .map { it.toApplication() }
    }

    suspend fun findById(id: String): JobApplication? = dbQuery {
        ApplicationsTable.selectAll().where { ApplicationsTable.id eq id }.singleOrNull()?.toApplication()
    }

    suspend fun hasApplied(jobId: String, applicantId: String): Boolean = dbQuery {
        ApplicationsTable.selectAll()
            .where { (ApplicationsTable.jobId eq jobId) and (ApplicationsTable.applicantId eq applicantId) }
            .count() > 0
    }

    suspend fun create(jobId: String, request: CreateApplicationRequest, applicant: User): JobApplication = dbQuery {
        val id = UUID.randomUUID().toString()
        val appliedAt = now()
        ApplicationsTable.insert {
            it[ApplicationsTable.id]             = id
            it[ApplicationsTable.jobId]          = jobId
            it[ApplicationsTable.applicantId]    = applicant.id
            it[ApplicationsTable.applicantName]  = applicant.name
            it[ApplicationsTable.applicantEmail] = applicant.email
            it[ApplicationsTable.coverLetter]    = request.coverLetter
            it[ApplicationsTable.resumeUrl]      = request.resumeUrl
            it[ApplicationsTable.status]         = ApplicationStatus.PENDING.name
            it[ApplicationsTable.appliedAt]      = appliedAt
        }
        JobApplication(
            id             = id,
            jobId          = jobId,
            applicantId    = applicant.id,
            applicantName  = applicant.name,
            applicantEmail = applicant.email,
            coverLetter    = request.coverLetter,
            resumeUrl      = request.resumeUrl,
            status         = ApplicationStatus.PENDING,
            appliedAt      = appliedAt.toString()
        )
    }

    suspend fun updateStatus(id: String, status: ApplicationStatus): JobApplication? = dbQuery {
        ApplicationsTable.update({ ApplicationsTable.id eq id }) {
            it[ApplicationsTable.status] = status.name
        }
        ApplicationsTable.selectAll().where { ApplicationsTable.id eq id }.singleOrNull()?.toApplication()
    }

    suspend fun deleteByJob(jobId: String): Unit = dbQuery {
        ApplicationsTable.deleteWhere { ApplicationsTable.jobId eq jobId }
    }
}

// ── User Repository ───────────────────────────────────────────────────────────

class UserRepository {

    suspend fun findById(id: String): User? = dbQuery {
        UsersTable.selectAll().where { UsersTable.id eq id }.singleOrNull()?.toUser()
    }

    suspend fun findByEmail(email: String): User? = dbQuery {
        UsersTable.selectAll().where { UsersTable.email eq email }.singleOrNull()?.toUser()
    }

    suspend fun emailExists(email: String): Boolean = dbQuery {
        UsersTable.selectAll().where { UsersTable.email eq email }.count() > 0
    }

    suspend fun create(request: RegisterRequest, passwordHash: String): User = dbQuery {
        val id = UUID.randomUUID().toString()
        UsersTable.insert {
            it[UsersTable.id]           = id
            it[UsersTable.name]         = request.name
            it[UsersTable.email]        = request.email
            it[UsersTable.passwordHash] = passwordHash
            it[UsersTable.role]         = request.role.name
            it[UsersTable.createdAt]    = now()
        }
        User(
            id           = id,
            name         = request.name,
            email        = request.email,
            role         = request.role,
            passwordHash = passwordHash
        )
    }
}