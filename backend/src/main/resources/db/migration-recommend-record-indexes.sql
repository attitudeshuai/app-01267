-- 推荐记录表索引迁移：为 recommend_record 添加查询性能索引
-- 适用于已有数据库，执行前请备份。若表由最新 schema.sql 创建则无需执行

-- 联合索引：按 purchaser_id + medicine_id 查询（recordRecommendClick、recordRecommendOrdered 等）
-- 若已存在则跳过（MySQL 8.0.13+ 支持 IF NOT EXISTS）
ALTER TABLE recommend_record ADD INDEX idx_purchaser_medicine (purchaser_id, medicine_id);

-- 时间索引：按 recommend_time 排序
ALTER TABLE recommend_record ADD INDEX idx_recommend_time (recommend_time);
