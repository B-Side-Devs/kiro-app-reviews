#!/usr/bin/env bash
# dev-down.sh - Stop the local development environment
# Preserves named volumes (postgres-data, backend-m2-cache)
#
# Usage: ./dev-down.sh

set -euo pipefail

# Resolve repository root (scripts/ -> docker/ -> infra/ -> repo-root)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
ENV_FILE="${REPO_ROOT}/.env"
COMPOSE_DIR="${SCRIPT_DIR}/.."

# Run docker compose down (without -v to preserve named volumes)
cd "${COMPOSE_DIR}"
docker compose --env-file "${ENV_FILE}" down
