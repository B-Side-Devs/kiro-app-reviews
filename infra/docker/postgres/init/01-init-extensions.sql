-- PostgreSQL initialization script for the local development environment.
--
-- This script is mounted read-only into the postgres container at
-- /docker-entrypoint-initdb.d/ and is executed automatically by the
-- official postgres image the first time the persistent data volume
-- is initialized (see Requirements 12.1, 12.2).
--
-- It enables the extensions required by the application on the
-- configured database (Requirement 12.3):
--   * uuid-ossp -> UUID generation helpers (uuid_generate_v4, ...)
--   * pg_trgm   -> trigram-based similarity / GIN indexes for search
--   * pgcrypto  -> cryptographic primitives (gen_random_uuid, digest, ...)
--
-- All statements use IF NOT EXISTS so the script is idempotent and safe
-- to keep in the initialization directory across re-initializations.

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
