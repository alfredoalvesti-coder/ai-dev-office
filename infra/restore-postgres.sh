#!/bin/sh
set -eu

if [ "$#" -ne 2 ] || [ ! -s "$1" ]; then
  echo "Uso: sh infra/restore-postgres.sh <backup> <banco novo office_restore_...>" >&2
  exit 2
fi

case "$2" in
  office_restore_?*) ;;
  *) echo "Use um banco novo com prefixo office_restore_." >&2; exit 2 ;;
esac
case "$2" in
  *[!a-z0-9_]*)
    echo "Nome do banco inválido." >&2
    exit 2
    ;;
esac
test "${#2}" -le 63

compose_file=$(dirname "$0")/compose.yml
docker compose -f "$compose_file" exec -T postgres sh -ec '
  target=$1
  test "$target" != "$POSTGRES_DB"
  createdb -U "$POSTGRES_USER" "$target"
  if ! pg_restore --exit-on-error --single-transaction --no-owner --no-acl -U "$POSTGRES_USER" -d "$target"; then
    dropdb -U "$POSTGRES_USER" "$target"
    exit 1
  fi
' restore "$2" < "$1"
