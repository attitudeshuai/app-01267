#!/bin/bash
# 数据库备份脚本 - 配合 cron 定时执行
# 用法: ./backup-db.sh 或 cron: 0 2 * * * /path/to/backup-db.sh

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-}"
DB_NAME="${DB_NAME:-medicine_sales}"
BACKUP_DIR="${BACKUP_DIR:-./backups}"
KEEP_DAYS="${KEEP_DAYS:-7}"

mkdir -p "$BACKUP_DIR"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/${DB_NAME}_${TIMESTAMP}.sql"

echo "[$(date)] Starting backup to $BACKUP_FILE"
mysqldump -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" ${DB_PASS:+-p"$DB_PASS"} \
  --single-transaction --routines --triggers \
  "$DB_NAME" > "$BACKUP_FILE" 2>/dev/null

if [ $? -eq 0 ]; then
  gzip -f "$BACKUP_FILE"
  echo "[$(date)] Backup completed: ${BACKUP_FILE}.gz"
  # 删除超过 KEEP_DAYS 天的备份
  find "$BACKUP_DIR" -name "*.sql.gz" -mtime +$KEEP_DAYS -delete 2>/dev/null
else
  echo "[$(date)] Backup failed!" >&2
  exit 1
fi
