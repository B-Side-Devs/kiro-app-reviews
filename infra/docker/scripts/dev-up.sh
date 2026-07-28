#!/usr/bin/env bash
# =============================================================================
# dev-up.sh — Start the local development environment (POSIX bash)
# =============================================================================
# This script starts all services defined in infra/docker/docker-compose.yml.
# It ensures the .env file exists at the repository root before invoking
# docker compose. If .env is missing, it copies from .env.example and exits
# with instructions to edit POSTGRES_PASSWORD.
#
# Usage:
#   ./infra/docker/scripts/dev-up.sh [additional docker compose up args...]
#
# Requirements: 1.1, 1.2, 1.4, 1.5, 8.4, 10.1, 10.2, 10.3, 16.1, 16.3
# =============================================================================

set -euo pipefail

# -----------------------------------------------------------------------------
# Resolve repository root (directory containing this script's parent's parent)
# -----------------------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
DOCKER_DIR="${REPO_ROOT}/infra/docker"

ENV_FILE="${REPO_ROOT}/.env"
ENV_EXAMPLE="${DOCKER_DIR}/.env.example"

# -----------------------------------------------------------------------------
# Verify .env exists; if missing, copy from template and exit with instruction
# -----------------------------------------------------------------------------
if [[ ! -f "${ENV_FILE}" ]]; then
    echo "ERROR: .env file not found at ${ENV_FILE}"
    echo ""
    echo "Creating .env from template..."

    if [[ ! -f "${ENV_EXAMPLE}" ]]; then
        echo "ERROR: Template file not found at ${ENV_EXAMPLE}"
        echo "Please ensure infra/docker/.env.example exists."
        exit 1
    fi

    cp "${ENV_EXAMPLE}" "${ENV_FILE}"
    echo "Created: ${ENV_FILE}"
    echo ""
    echo "=========================================="
    echo "ACTION REQUIRED"
    echo "=========================================="
    echo "1. Edit ${ENV_FILE}"
    echo "2. Replace 'CHANGE_ME' with a real password for POSTGRES_PASSWORD"
    echo "3. Re-run this script"
    echo "=========================================="
    exit 1
fi

# -----------------------------------------------------------------------------
# Verify POSTGRES_PASSWORD is set (not empty or CHANGE_ME)
# -----------------------------------------------------------------------------
# shellcheck disable=SC1090
source "${ENV_FILE}"

if [[ -z "${POSTGRES_PASSWORD:-}" ]] || [[ "${POSTGRES_PASSWORD}" == "CHANGE_ME" ]]; then
    echo "ERROR: POSTGRES_PASSWORD is not properly configured."
    echo ""
    echo "Please edit ${ENV_FILE} and set POSTGRES_PASSWORD to a real value."
    echo "The password does not need to be strong — it is for local development only."
    exit 1
fi

# -----------------------------------------------------------------------------
# Verify Docker is running
# -----------------------------------------------------------------------------
if ! docker info > /dev/null 2>&1; then
    echo "ERROR: Docker Engine is not running."
    echo ""
    echo "Please start Docker and re-run this script."
    exit 1
fi

# -----------------------------------------------------------------------------
# Invoke docker compose from the docker directory
# -----------------------------------------------------------------------------
cd "${DOCKER_DIR}"

echo "Starting development environment..."
echo ""

# Pass any additional arguments to docker compose up (e.g., --build)
docker compose --env-file "${ENV_FILE}" up -d "$@"

echo ""
echo "Service status:"
docker compose --env-file "${ENV_FILE}" ps
