#!/bin/bash
# infra/postgres/init/01-init-databases.sh
# Crée les bases additionnelles déclarées dans POSTGRES_MULTIPLE_DATABASES
set -e

create_database() {
  local db=$1
  echo "  Création de la base : $db"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    SELECT 'CREATE DATABASE $db OWNER $POSTGRES_USER'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$db')\gexec
EOSQL
}

if [ -n "$POSTGRES_MULTIPLE_DATABASES" ]; then
  echo "==> Initialisation des bases multiples : $POSTGRES_MULTIPLE_DATABASES"
  for db in $(echo $POSTGRES_MULTIPLE_DATABASES | tr ',' ' '); do
    create_database "$db"
  done
fi
