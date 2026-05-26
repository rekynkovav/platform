#!/bin/bash
BACKUP_DIR="/backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
DB_NAME="platform"
DB_USER="platform"
DB_PASS="platform123"

mkdir -p $BACKUP_DIR

PGPASSWORD=$DB_PASS pg_dump -h postgres -U $DB_USER $DB_NAME > $BACKUP_DIR/backup_$TIMESTAMP.sql

# Удаляем старые бэкапы (старше 30 дней)
find $BACKUP_DIR -name "backup_*.sql" -mtime +30 -delete

echo "Backup completed: backup_$TIMESTAMP.sql"