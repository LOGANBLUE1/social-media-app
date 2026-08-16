#!/usr/bin/env bash
#
# Nightly database dump. Postgres lives in a container on the same box as the API, so its data is a
# Docker volume on the instance's EBS root -- a stop/start keeps it, but terminating the instance
# destroys it. This is the difference between that being an inconvenience and a disaster.
#
# Install on the server:
#   chmod +x ~/social-media-app/server/scripts/backup.sh
#   crontab -e
#   0 3 * * * /home/ubuntu/social-media-app/server/scripts/backup.sh >> /home/ubuntu/backup.log 2>&1
#
# Set S3_BUCKET to copy off-box as well. Without it the dumps sit on the same disk as the database,
# which protects against "I dropped a table" but not against losing the instance.

set -euo pipefail

COMPOSE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKUP_DIR="${BACKUP_DIR:-$HOME/backups}"
KEEP_DAYS="${KEEP_DAYS:-7}"
S3_BUCKET="${S3_BUCKET:-}"

cd "$COMPOSE_DIR"
# Read the credentials from the same .env the stack uses, rather than duplicating them here.
set -a; . ./.env; set +a

mkdir -p "$BACKUP_DIR"
STAMP=$(date +%Y%m%d-%H%M%S)
FILE="$BACKUP_DIR/${DB_NAME}-${STAMP}.sql.gz"

docker compose exec -T postgres pg_dump -U "$DB_USERNAME" -d "$DB_NAME" | gzip > "$FILE"

# A pg_dump that failed early still leaves a valid-looking gzip, so check the dump actually has a
# terminator in it before trusting this backup and rotating older ones away.
if ! gzip -dc "$FILE" | tail -5 | grep -q "PostgreSQL database dump complete"; then
    echo "$(date -Is) FAILED: $FILE is truncated, keeping older backups" >&2
    rm -f "$FILE"
    exit 1
fi

echo "$(date -Is) wrote $FILE ($(du -h "$FILE" | cut -f1))"

if [ -n "$S3_BUCKET" ]; then
    aws s3 cp "$FILE" "s3://$S3_BUCKET/db/" --only-show-errors
    echo "$(date -Is) uploaded to s3://$S3_BUCKET/db/"
fi

find "$BACKUP_DIR" -name "${DB_NAME}-*.sql.gz" -mtime "+$KEEP_DAYS" -delete
