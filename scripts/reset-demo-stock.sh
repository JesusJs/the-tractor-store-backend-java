#!/usr/bin/env bash
# Restores inventory_stock to the V4 seed in the local dev database (review 004 #2): run it before
# the demo or after someone empties the stock. Orders are kept. Needs psql on the host.
#
#   scripts/reset-demo-stock.sh                 # 127.0.0.1:55432, database/user tractor
#   DB_PORT=5432 scripts/reset-demo-stock.sh    # same variables as compose.yaml and the dev profile
set -euo pipefail

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-55432}"
DB_NAME="${DB_NAME:-tractor}"
DB_USER="${DB_USER:-tractor}"
# Public dev default of compose.yaml; never a real secret.
export PGPASSWORD="${DB_PASSWORD:-tractor}"

case "$DB_HOST" in
  127.0.0.1 | localhost | ::1) ;;
  *)
    echo "Refusing to reset a non local database ($DB_HOST): this script is for the dev one." >&2
    exit 1
    ;;
esac

seed="$(dirname "$0")/../src/main/resources/db/migration/V4__inventory_stock.sql"
# The INSERT of the migration is the single source of the seed values.
insert="$(sed -n '/^INSERT INTO inventory_stock/,/;$/p' "$seed")"
if [[ -z "$insert" ]]; then
  echo "No INSERT INTO inventory_stock found in $seed" >&2
  exit 1
fi

psql -v ON_ERROR_STOP=1 -q -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" <<SQL
BEGIN;
DELETE FROM inventory_stock;
$insert
COMMIT;
SQL

psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -At -c \
  "select count(*) || ' SKUs restored; CL-01-GR=' || max(stock) filter (where sku = 'CL-01-GR')
     || ', CL-01-GY=' || max(stock) filter (where sku = 'CL-01-GY')
     || ', AU-08-WH=' || max(stock) filter (where sku = 'AU-08-WH')
     || ', AU-07-YE=' || max(stock) filter (where sku = 'AU-07-YE') from inventory_stock"
