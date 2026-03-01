CREATE TABLE IF NOT EXISTS users (
    id            VARCHAR(36)  PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(64)  NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('EMPLOYER', 'APPLICANT')),
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jobs (
    id           VARCHAR(36)  PRIMARY KEY,
    title        VARCHAR(200) NOT NULL,
    company      VARCHAR(100) NOT NULL,
    location     VARCHAR(100) NOT NULL,
    type         VARCHAR(20)  NOT NULL CHECK (type IN ('FULL_TIME', 'PART_TIME', 'CONTRACT', 'INTERNSHIP', 'REMOTE')),
    description  TEXT         NOT NULL,
    requirements TEXT         NOT NULL,
    salary_min   INTEGER,
    salary_max   INTEGER,
    status       VARCHAR(20)  NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'CLOSED')),
    posted_by    VARCHAR(36)  NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    posted_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_jobs_status    ON jobs(status);
CREATE INDEX idx_jobs_posted_by ON jobs(posted_by);
CREATE INDEX idx_jobs_posted_at ON jobs(posted_at DESC);

CREATE TABLE IF NOT EXISTS applications (
    id              VARCHAR(36)  PRIMARY KEY,
    job_id          VARCHAR(36)  NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    applicant_id    VARCHAR(36)  NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    applicant_name  VARCHAR(100) NOT NULL,
    applicant_email VARCHAR(255) NOT NULL,
    cover_letter    TEXT         NOT NULL,
    resume_url      VARCHAR(500),
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING', 'REVIEWED', 'SHORTLISTED', 'REJECTED', 'HIRED')),
    applied_at      TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_job_applicant UNIQUE (job_id, applicant_id)
);

CREATE INDEX idx_applications_job_id       ON applications(job_id);
CREATE INDEX idx_applications_applicant_id ON applications(applicant_id);
