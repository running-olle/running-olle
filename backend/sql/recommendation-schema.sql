-- Running Olle home recommendation schema changes
-- Apply this to shared databases before enabling home.recommendation.rag.enabled=true.

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS course_recommendation_documents (
    id uuid PRIMARY KEY,
    course_id uuid NOT NULL,
    source_type varchar(40) NOT NULL,
    source_key varchar(120) NOT NULL,
    title varchar(200),
    content text NOT NULL,
    metadata jsonb,
    embedding_model varchar(120),
    embedding_status varchar(20) NOT NULL DEFAULT 'PENDING',
    embedded_at timestamp,
    embedding_failure_reason varchar(500),
    is_deleted boolean NOT NULL DEFAULT false,
    deleted_at timestamp,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT fk_course_recommendation_documents_course
        FOREIGN KEY (course_id) REFERENCES courses(id),
    CONSTRAINT uk_course_recommendation_documents_course_id_source_type_source_key
        UNIQUE (course_id, source_type, source_key)
);

CREATE INDEX IF NOT EXISTS idx_course_recommendation_documents_course_id
    ON course_recommendation_documents(course_id);

CREATE INDEX IF NOT EXISTS idx_course_recommendation_documents_embedding_status
    ON course_recommendation_documents(embedding_status);
