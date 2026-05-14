COMPOSE_FILE := infrastructure/docker/docker-compose.dev.yml
ENV_FILE := infrastructure/docker/.env
ENV_EXAMPLE := infrastructure/docker/.env.example
COMPOSE := docker compose --env-file $(ENV_FILE) -f $(COMPOSE_FILE)

ifeq ($(OS),Windows_NT)
SHELL := cmd.exe
.SHELLFLAGS := /C
ENV_FILE_NATIVE := infrastructure\docker\.env
ENV_EXAMPLE_NATIVE := infrastructure\docker\.env.example
ENSURE_ENV = if not exist "$(ENV_FILE_NATIVE)" (copy "$(ENV_EXAMPLE_NATIVE)" "$(ENV_FILE_NATIVE)" >NUL && echo Created $(ENV_FILE) from $(ENV_EXAMPLE). Review secrets before using outside local development.)
else
SHELL := /bin/bash
.SHELLFLAGS := -eu -o pipefail -c
ENSURE_ENV = if [ ! -f "$(ENV_FILE)" ]; then cp "$(ENV_EXAMPLE)" "$(ENV_FILE)"; echo "Created $(ENV_FILE) from $(ENV_EXAMPLE). Review secrets before using outside local development."; fi
endif

.PHONY: dev-env dev-up dev-down dev-reset dev-logs dev-ps dev-config

dev-env:
	$(ENSURE_ENV)

dev-up: dev-env
	$(COMPOSE) up -d --wait postgres keycloak kafka kafka-ui

dev-down: dev-env
	$(COMPOSE) down

dev-reset: dev-env
	$(COMPOSE) down -v --remove-orphans
	$(COMPOSE) up -d --wait postgres keycloak kafka kafka-ui

dev-logs: dev-env
	$(COMPOSE) logs -f --tail=200

dev-ps: dev-env
	$(COMPOSE) ps

dev-config: dev-env
	$(COMPOSE) config
