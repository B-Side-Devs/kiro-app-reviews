#!/usr/bin/env bash
# dev-logs.sh - View logs from the local development environment
# Follows logs from all services, or a specific service if provided
#
# Usage: ./dev-logs.sh [service]
# Examples:
#   ./dev-logs.sh           # Follow logs from all services
#   ./dev-logs.sh postgres  # Follow logs from postgres service
#   ./dev-logs.sh backend   # Follow logs from backend service

set -euo pipefail

# Resolve repository root (scripts/ -> docker/ -> infra/ -> repo-root)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
ENV_FILE="${REPO_ROOT}/.env"
COMPOSE_DIR="${SCRIPT_DIR}/.."

# Change to compose directory
cd "${COMPOSE_DIR}"

# If a service name is provided, filter to that service
if [ $# -gt 0 ]; then
    docker compose --env-file "${ENV_FILE}" logs -f "$@"
else
    docker compose --env-file "${ENV_FILE}" logs -f
fi
