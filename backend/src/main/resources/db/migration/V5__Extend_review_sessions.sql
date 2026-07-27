-- V5: Extend review_sessions table with metadata, optimistic locking,
--     CHECK constraints, TIMESTAMPTZ(3) conversion, listing index, and immutable project_id trigger.

-- 1. Add metadata columns (nullable)
ALTER TABLE review_sessions ADD COLUMN name VARCHAR(200);
ALTER TABLE review_sessions ADD COLUMN description VARCHAR(2000);
ALTER TABLE review_sessions ADD COLUMN target_url VARCHAR(2048);

-- 2. Add version column for optimistic locking
ALTER TABLE review_sessions ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- 3. Add CHECK constraint for state
ALTER TABLE review_sessions
    ADD CONSTRAINT chk_review_sessions_state
    CHECK (state IN ('DRAFT', 'RECORDING', 'COMPLETED', 'REOPENED', 'ARCHIVED', 'DELETED'));

-- 4. Add CHECK constraint for persistence_status
ALTER TABLE review_sessions
    ADD CONSTRAINT chk_review_sessions_persistence_status
    CHECK (persistence_status IN ('OK', 'IN_PROGRESS', 'FAILED'));

-- 5. Convert all timestamp columns from TIMESTAMP to TIMESTAMPTZ(3)
ALTER TABLE review_sessions ALTER COLUMN created_at TYPE TIMESTAMPTZ(3);
ALTER TABLE review_sessions ALTER COLUMN started_recording_at TYPE TIMESTAMPTZ(3);
ALTER TABLE review_sessions ALTER COLUMN completed_at TYPE TIMESTAMPTZ(3);
ALTER TABLE review_sessions ALTER COLUMN reopened_at TYPE TIMESTAMPTZ(3);
ALTER TABLE review_sessions ALTER COLUMN archived_at TYPE TIMESTAMPTZ(3);
ALTER TABLE review_sessions ALTER COLUMN deleted_at TYPE TIMESTAMPTZ(3);

-- 6. Create composite index for project listing (project_id, state, created_at DESC, review_session_id ASC)
CREATE INDEX idx_review_sessions_project_listing
    ON review_sessions(project_id, state, created_at DESC, review_session_id ASC);

-- 7. Trigger function to prevent project_id modification on UPDATE
CREATE OR REPLACE FUNCTION fn_review_sessions_immutable_project_id()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.project_id IS DISTINCT FROM OLD.project_id THEN
        RAISE EXCEPTION 'Cannot modify project_id of an existing review session'
            USING ERRCODE = '23514'; -- check_violation
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_review_sessions_immutable_project_id
    BEFORE UPDATE ON review_sessions
    FOR EACH ROW
    EXECUTE FUNCTION fn_review_sessions_immutable_project_id();
