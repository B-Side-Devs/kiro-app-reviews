CREATE TABLE users (
    user_id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    role VARCHAR(50) NOT NULL,
    display_name VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Seed user for local/Postman testing
INSERT INTO users (user_id, email, role, display_name, created_at)
VALUES (
    '018e0c5a-7b3f-7000-8000-000000000001',
    'test@example.com',
    'PROJECT_ADMIN',
    'Test User',
    NOW()
);
