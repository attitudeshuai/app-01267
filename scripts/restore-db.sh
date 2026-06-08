#!/bin/bash
# 数据库恢复脚本
# 用法: ./restore-db.sh <backup_file.sql.gz>
# 示例: ./restore-db.sh ./backups/medicine_sales_20260228_020000.sql.gz

if [ $# -lt 1 ]; then
  echo "Usage: $0 <backup_file.sql.gz>" >&2
  echo "Example: $0 ./backups/medicine_sales_20260228_020000.sql.gz" >&2
  exit 1
fi

BACKUP_FILE="$1"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-}"
DB_NAME="${DB_NAME:-medicine_sales}"

if [ ! -f "$BACKUP_FILE" ]; then
  echo "File not found: $BACKUP_FILE" >&2
  exit 1
fi

echo "[$(date)] Restoring from $BACKUP_FILE to $DB_NAME"
if [[ "$BACKUP_FILE" == *.gz ]]; then
  gunzip -c "$BACKUP_FILE" | mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" ${DB_PASS:+-p"$DB_PASS"} "$DB_NAME"
else
  mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" ${DB_PASS:+-p"$DB_PASS"} "$DB_NAME" < "$BACKUP_FILE"
fi

if [ $? -eq 0 ]; then
  echo "[$(date)] Restore completed successfully"
else
  echo "[$(date)] Restore failed!" >&2
  exit 1
fi
