CREATE TABLE projects (
    project_id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(workspace_id),
    owner_admin_id UUID NOT NULL REFERENCES users(user_id),
    display_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Seed project for local/Postman testing
INSERT INTO projects (project_id, workspace_id, owner_admin_id, display_name, created_at)
VALUES (
    '018e0c5c-2b3c-7000-8000-000000000001',
    '018e0c5b-1a2b-7000-8000-000000000001',
    '018e0c5a-7b3f-7000-8000-000000000001',
    'Test Project',
    NOW()
);
