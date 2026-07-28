-- V8: Create scenes table.
--
-- Persists a single Excalidraw Scene per Review Session (1:1). The Scene is
-- stored as an opaque JSON document following Excalidraw's official
-- `.excalidraw` public format; the backend never interprets its internal
-- structure (elements, appState, files).
--
-- The 1:1 relationship is enforced structurally: review_session_id is BOTH the
-- primary key AND a foreign key to review_sessions. Each save fully replaces the
-- previous document (upsert); no history is kept in this iteration.

CREATE TABLE scenes (
    review_session_id UUID PRIMARY KEY
        REFERENCES review_sessions(review_session_id),
    document          JSONB          NOT NULL,
    version           BIGINT         NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ(3) NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ(3) NOT NULL DEFAULT NOW()
);
