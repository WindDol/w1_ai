-- ScholarBrain phase 2: structure-aware chunk retrieval index.
-- Run this once before rebuilding papers from the EMBEDDING stage.

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS section_chunks (
    id                  VARCHAR(64) PRIMARY KEY,
    paper_id            BIGINT NOT NULL REFERENCES papers(id) ON DELETE CASCADE,
    section_id          VARCHAR(64) NOT NULL REFERENCES sections(id) ON DELETE CASCADE,
    chunk_index         INTEGER NOT NULL,
    heading             TEXT,
    heading_path        TEXT,
    content             TEXT NOT NULL,
    page_start          INTEGER,
    page_end            INTEGER,
    token_count         INTEGER NOT NULL DEFAULT 0,
    content_hash        VARCHAR(64) NOT NULL,
    chunker_version     VARCHAR(64) NOT NULL,
    embedding_model     VARCHAR(128),
    embedding           vector(1536),
    search_vector       tsvector GENERATED ALWAYS AS (
        setweight(to_tsvector('english'::regconfig, coalesce(heading_path, '')), 'A') ||
        setweight(to_tsvector('english'::regconfig, coalesce(content, '')), 'B')
    ) STORED,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_section_chunks_section_index UNIQUE (section_id, chunk_index)
);

CREATE INDEX IF NOT EXISTS idx_section_chunks_paper
    ON section_chunks (paper_id);

CREATE INDEX IF NOT EXISTS idx_section_chunks_section
    ON section_chunks (section_id);

CREATE INDEX IF NOT EXISTS idx_section_chunks_search_vector
    ON section_chunks USING GIN (search_vector);

-- Requires a pgvector version with HNSW support. If unavailable, omit this index;
-- retrieval remains correct and only loses approximate-nearest-neighbor acceleration.
CREATE INDEX IF NOT EXISTS idx_section_chunks_embedding_hnsw
    ON section_chunks USING hnsw (embedding vector_cosine_ops);
