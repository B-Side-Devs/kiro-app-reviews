# Makefile - Local Development Environment
# Convenience targets for docker compose operations
# Requirements: 10.6

DOCKER_COMPOSE := docker compose
ENV_FILE := .env
COMPOSE_DIR := infra/docker

.PHONY: up down reset logs backend-shell db-shell ps

# Start the development environment
up:
	cd $(COMPOSE_DIR) && $(DOCKER_COMPOSE) --env-file ../../$(ENV_FILE) up -d
	cd $(COMPOSE_DIR) && $(DOCKER_COMPOSE) --env-file ../../$(ENV_FILE) ps

# Stop the development environment (preserves volumes)
down:
	cd $(COMPOSE_DIR) && $(DOCKER_COMPOSE) --env-file ../../$(ENV_FILE) down

# Reset: stop and remove all volumes (DESTROYS DATA)
reset:
	cd $(COMPOSE_DIR) && $(DOCKER_COMPOSE) --env-file ../../$(ENV_FILE) down -v --remove-orphans

# Follow logs of all services or a specific service (SERVICE=backend|postgres)
logs:
ifdef SERVICE
	cd $(COMPOSE_DIR) && $(DOCKER_COMPOSE) --env-file ../../$(ENV_FILE) logs -f $(SERVICE)
else
	cd $(COMPOSE_DIR) && $(DOCKER_COMPOSE) --env-file ../../$(ENV_FILE) logs -f
endif

# Open a shell in the backend container
backend-shell:
	cd $(COMPOSE_DIR) && $(DOCKER_COMPOSE) --env-file ../../$(ENV_FILE) exec backend /bin/bash

# Open a shell in the database container
db-shell:
	cd $(COMPOSE_DIR) && $(DOCKER_COMPOSE) --env-file ../../$(ENV_FILE) exec postgres /bin/bash

# Show status of all containers
ps:
	cd $(COMPOSE_DIR) && $(DOCKER_COMPOSE) --env-file ../../$(ENV_FILE) ps
