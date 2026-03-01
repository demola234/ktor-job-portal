package com.example.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object UsersTable : Table("users") {
    val id           = varchar("id", 36)
    val name         = varchar("name", 100)
    val email        = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 64)
    val role         = varchar("role", 20)
    val createdAt    = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}

object JobsTable : Table("jobs") {
    val id           = varchar("id", 36)
    val title        = varchar("title", 200)
    val company      = varchar("company", 100)
    val location     = varchar("location", 100)
    val type         = varchar("type", 20)
    val description  = text("description")
    val requirements = text("requirements")
    val salaryMin    = integer("salary_min").nullable()
    val salaryMax    = integer("salary_max").nullable()
    val status       = varchar("status", 20)
    val postedBy     = varchar("posted_by", 36).references(UsersTable.id)
    val postedAt     = timestamp("posted_at")

    override val primaryKey = PrimaryKey(id)
}

object ApplicationsTable : Table("applications") {
    val id             = varchar("id", 36)
    val jobId          = varchar("job_id", 36).references(JobsTable.id)
    val applicantId    = varchar("applicant_id", 36).references(UsersTable.id)
    val applicantName  = varchar("applicant_name", 100)
    val applicantEmail = varchar("applicant_email", 255)
    val coverLetter    = text("cover_letter")
    val resumeUrl      = varchar("resume_url", 500).nullable()
    val status         = varchar("status", 20)
    val appliedAt      = timestamp("applied_at")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("uq_job_applicant", jobId, applicantId)
    }
}
