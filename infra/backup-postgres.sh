#!/bin/sh
set -eu
umask 077

if [ "$#" -ne 1 ] || [ -e "$1" ]; then
  echo "Uso: sh infra/backup-postgres.sh <arquivo novo>" >&2
  exit 2
fi

compose_file=$(dirname "$0")/compose.yml
temp=$(mktemp -- "$1.XXXXXX")
trap 'rm -f -- "$temp"' EXIT
docker compose -f "$compose_file" exec -T postgres sh -ec '
  pg_dump --format=custom --no-owner --no-acl -U "$POSTGRES_USER" -d "$POSTGRES_DB"
' > "$temp"
test -s "$temp"
ln -T -- "$temp" "$1"
