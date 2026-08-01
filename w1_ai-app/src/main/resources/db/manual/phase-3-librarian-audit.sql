-- ScholarBrain phase 3B: versioned, evidence-backed librarian relation audits.
-- Run this once after phase-2-retrieval-index.sql and before enabling the new LIBRARIAN_AUDIT flow.

ALTER TABLE paper_knowledge_relations
    ADD COLUMN IF NOT EXISTS confidence DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS audit_status VARCHAR(32) NOT NULL DEFAULT 'LEGACY',
    ADD COLUMN IF NOT EXISTS supporting_evidence TEXT NOT NULL DEFAULT '[]',
    ADD COLUMN IF NOT EXISTS conflicting_evidence TEXT NOT NULL DEFAULT '[]',
    ADD COLUMN IF NOT EXISTS audit_version VARCHAR(64) NOT NULL DEFAULT 'legacy-v0',
    ADD COLUMN IF NOT EXISTS model_name VARCHAR(128),
    ADD COLUMN IF NOT EXISTS prompt_version VARCHAR(64),
    ADD COLUMN IF NOT EXISTS retrieval_version VARCHAR(256),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- A re-run replaces the row for the same source/target/audit version instead of accumulating duplicates.
CREATE INDEX IF NOT EXISTS idx_paper_relation_audit_version
    ON paper_knowledge_relations (source_paper_id, target_paper_id, audit_version);

-- Existing rows predate evidence capture. They remain readable but are explicitly marked as legacy.
UPDATE paper_knowledge_relations
SET audit_status = 'LEGACY',
    audit_version = COALESCE(NULLIF(audit_version, ''), 'legacy-v0'),
    supporting_evidence = COALESCE(supporting_evidence, '[]'),
    conflicting_evidence = COALESCE(conflicting_evidence, '[]')
WHERE audit_status IS NULL
   OR audit_version IS NULL
   OR supporting_evidence IS NULL
   OR conflicting_evidence IS NULL;
