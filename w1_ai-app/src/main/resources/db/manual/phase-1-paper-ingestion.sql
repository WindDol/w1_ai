-- ScholarBrain Phase 1: durable paper ingestion workflow
-- Run this once against the existing w1_ai PostgreSQL database before starting the application.

CREATE TABLE IF NOT EXISTS paper_ingest_jobs (
    id                          VARCHAR(36) PRIMARY KEY,
    paper_id                    BIGINT NULL REFERENCES papers(id) ON DELETE SET NULL,
    original_filename           VARCHAR(512) NOT NULL,
    file_sha256                 CHAR(64) NOT NULL,
    file_size                   BIGINT NOT NULL,
    source_file_path            TEXT NOT NULL,
    status                      VARCHAR(32) NOT NULL,
    current_stage               VARCHAR(64) NOT NULL,
    failed_stage                VARCHAR(64),
    error_code                  VARCHAR(128),
    error_message               TEXT,
    attempt_count               INTEGER NOT NULL DEFAULT 1,
    parser_type                 VARCHAR(64),
    parser_version              VARCHAR(128),
    normalizer_version          VARCHAR(128),
    parser_artifact_path        TEXT,
    raw_markdown_path           TEXT,
    normalized_markdown_path    TEXT,
    normalization_report_path   TEXT,
    metadata_path               TEXT,
    extracted_title             TEXT,
    extracted_abstract          TEXT,
    paper_fingerprint           VARCHAR(255),
    publication_year            INTEGER,
    worker_token                VARCHAR(36),
    lease_until                 TIMESTAMP,
    version                     BIGINT NOT NULL DEFAULT 0,
    created_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at                  TIMESTAMP,
    completed_at                TIMESTAMP,
    CONSTRAINT uk_paper_ingest_jobs_sha256 UNIQUE (file_sha256),
    CONSTRAINT ck_paper_ingest_jobs_status CHECK (
        status IN ('UPLOADED', 'PARSING', 'PARSED', 'AUDITING', 'READY', 'FAILED')
    )
);

CREATE INDEX IF NOT EXISTS idx_paper_ingest_jobs_recovery
    ON paper_ingest_jobs (status, updated_at);

CREATE INDEX IF NOT EXISTS idx_paper_ingest_jobs_paper_id
    ON paper_ingest_jobs (paper_id);

CREATE TABLE IF NOT EXISTS paper_ingest_stage_runs (
    id              BIGSERIAL PRIMARY KEY,
    job_id          VARCHAR(36) NOT NULL REFERENCES paper_ingest_jobs(id) ON DELETE CASCADE,
    stage           VARCHAR(64) NOT NULL,
    attempt         INTEGER NOT NULL,
    status          VARCHAR(32) NOT NULL,
    artifact_path   TEXT,
    error_code      VARCHAR(128),
    error_message   TEXT,
    started_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at     TIMESTAMP,
    CONSTRAINT ck_paper_ingest_stage_runs_status CHECK (
        status IN ('RUNNING', 'SUCCEEDED', 'FAILED')
    )
);

CREATE INDEX IF NOT EXISTS idx_paper_ingest_stage_runs_job
    ON paper_ingest_stage_runs (job_id, attempt, id);

