#!/usr/bin/env bash
set -euo pipefail

keycloak_db="${MIREMS_KEYCLOAK_DB_NAME:-keycloak}"

if [[ "${keycloak_db}" == "${POSTGRES_DB}" ]]; then
  echo "Keycloak database matches POSTGRES_DB (${POSTGRES_DB}); skipping separate database creation."
  exit 0
fi

if psql --username "${POSTGRES_USER}" --dbname "${POSTGRES_DB}" -tAc "SELECT 1 FROM pg_database WHERE datname='${keycloak_db}'" | grep -q 1; then
  echo "Database ${keycloak_db} already exists."
else
  psql --username "${POSTGRES_USER}" --dbname "${POSTGRES_DB}" -v ON_ERROR_STOP=1 -c "CREATE DATABASE \"${keycloak_db}\""
  echo "Created Keycloak database ${keycloak_db}."
fi
