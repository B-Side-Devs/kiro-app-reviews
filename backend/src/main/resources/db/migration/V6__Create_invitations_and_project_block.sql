-- V6: Add blocked column to projects and create invitations table
-- Requirements: 11.7, 11.15, 11.16

-- 11.7: Add blocked indicator to projects (default FALSE for existing and new rows)
ALTER TABLE projects
    ADD COLUMN blocked BOOLEAN NOT NULL DEFAULT FALSE;

-- 11.6 / 11.16: Create invitations table
CREATE TABLE invitations (
    invitation_id       UUID            PRIMARY KEY,
    project_id          UUID            NOT NULL REFERENCES projects(project_id),
    invitee_user_id     UUID            NOT NULL REFERENCES users(user_id),
    status              VARCHAR(50)     NOT NULL,
    issued_at           TIMESTAMPTZ(3)  NOT NULL,
    accepted_at         TIMESTAMPTZ(3),
    revoked_at          TIMESTAMPTZ(3),

    -- 11.15: Only one invitation per project + invitee combination
    CONSTRAINT uq_invitations_project_invitee UNIQUE (project_id, invitee_user_id),

    -- CHECK constraint for valid invitation statuses
    CONSTRAINT chk_invitation_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REVOKED', 'EXPIRED'))
);

-- Index for lookups by project and invitee
CREATE INDEX idx_invitations_project_invitee ON invitations(project_id, invitee_user_id);
