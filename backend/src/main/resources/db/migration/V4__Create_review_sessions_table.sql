-- Review Session state machine: DRAFT → RECORDING → COMPLETED ↔ REOPENED → ARCHIVED → DELETED
CREATE TABLE review_sessions (
    review_session_id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(project_id),
    created_by_user_id UUID NOT NULL REFERENCES users(user_id),
    state VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    persistence_status VARCHAR(50) NOT NULL DEFAULT 'OK',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    started_recording_at TIMESTAMP,
    completed_at TIMESTAMP,
    reopened_at TIMESTAMP,
    archived_at TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_review_sessions_project_id ON review_sessions(project_id);
CREATE INDEX idx_review_sessions_created_by ON review_sessions(created_by_user_id);
