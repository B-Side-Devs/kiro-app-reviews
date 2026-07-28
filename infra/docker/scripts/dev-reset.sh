#!/usr/bin/env bash
# dev-reset.sh - Stop the local development environment and remove all volumes
# WARNING: This destroys all data in postgres-data and backend-m2-cache volumes
#
# Usage: ./dev-reset.sh

set -euo pipefail

# Resolve repository root (scripts/ -> docker/ -> infra/ -> repo-root)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
ENV_FILE="${REPO_ROOT}/.env"
COMPOSE_DIR="${SCRIPT_DIR}/.."

# Prompt for explicit confirmation
echo "WARNING: This will stop all services and DELETE ALL DATA:"
echo "  - postgres-data volume (database)"
echo "  - backend-m2-cache volume (Maven dependencies)"
echo ""
echo "Type RESET and press Enter to confirm, or anything else to cancel:"
read -r CONFIRM

if [[ "${CONFIRM}" != "RESET" ]]; then
    echo "Reset cancelled. No changes were made."
    exit 1
fi

# Run docker compose down with volume removal
cd "${COMPOSE_DIR}"
docker compose --env-file "${ENV_FILE}" down -v --remove-orphans

echo ""
echo "Reset complete. All volumes have been removed."
