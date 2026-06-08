-- 新增推荐算法相关配置项（用于已存在的数据库，执行前请确认 system_config 表存在）
INSERT IGNORE INTO system_config (config_key, config_value, description) VALUES
('recommend_similar_count', '10', '协同过滤相似用户数量上限'),
('recommend_item_count', '10', '推荐结果数量'),
('recommend_cache_hours', '1', '推荐缓存有效期(小时)');
