-- V7: Seed authorization actors for local/Postman testing
-- Requirements: 11.9, 11.10, 11.16
--
-- Fixed UUIDs (v7 format) documented for Postman collection:
--   CLIENT user:          018e0c5a-7b3f-7000-8000-000000000002
--   PROJECT_ADMIN (other):018e0c5a-7b3f-7000-8000-000000000003
--   PLATFORM_ADMIN:       018e0c5a-7b3f-7000-8000-000000000004
--   Invitation (CLIENT):  018e0c5d-4d5e-7000-8000-000000000001

-- 11.9: Insert CLIENT user (distinct from existing PROJECT_ADMIN owner)
INSERT INTO users (user_id, email, role, display_name, created_at)
VALUES (
    '018e0c5a-7b3f-7000-8000-000000000002',
    'client@example.com',
    'CLIENT',
    'Test Client',
    NOW()
) ON CONFLICT DO NOTHING;

-- 11.9: Insert PROJECT_ADMIN user (non-owner, distinct from seed owner)
INSERT INTO users (user_id, email, role, display_name, created_at)
VALUES (
    '018e0c5a-7b3f-7000-8000-000000000003',
    'other-admin@example.com',
    'PROJECT_ADMIN',
    'Other Admin',
    NOW()
) ON CONFLICT DO NOTHING;

-- 11.9: Insert PLATFORM_ADMIN user
INSERT INTO users (user_id, email, role, display_name, created_at)
VALUES (
    '018e0c5a-7b3f-7000-8000-000000000004',
    'platform-admin@example.com',
    'PLATFORM_ADMIN',
    'Platform Admin',
    NOW()
) ON CONFLICT DO NOTHING;

-- 11.8: Insert ACCEPTED invitation from CLIENT to seed project
-- issued_at and accepted_at populated, revoked_at NULL
INSERT INTO invitations (invitation_id, project_id, invitee_user_id, status, issued_at, accepted_at, revoked_at)
VALUES (
    '018e0c5d-4d5e-7000-8000-000000000001',
    '018e0c5c-2b3c-7000-8000-000000000001',
    '018e0c5a-7b3f-7000-8000-000000000002',
    'ACCEPTED',
    '2024-01-15T10:00:00.000Z',
    '2024-01-15T10:05:00.000Z',
    NULL
) ON CONFLICT DO NOTHING;
