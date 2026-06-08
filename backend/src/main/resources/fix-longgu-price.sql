-- 清除龙骨异常价格数据（修复因填写超长数字导致的报错）
-- 使用方法：mysql -u root -p medicine_sales < fix-longgu-price.sql

USE medicine_sales;

-- 1. 将 medicine_info 中龙骨的价格重置为正常值
UPDATE medicine_info SET price = 0.18 WHERE name = '龙骨';

-- 2. 删除龙骨的价格记录（清除可能存在的异常数据）
DELETE pr FROM price_record pr
INNER JOIN medicine_info mi ON pr.medicine_id = mi.id
WHERE mi.name = '龙骨';

-- 3. 重新插入龙骨的价格历史记录
INSERT INTO price_record (medicine_id, merchant_id, price, is_abnormal, record_time)
SELECT id, 1, 0.16, 0, DATE_SUB(NOW(), INTERVAL 90 DAY) FROM medicine_info WHERE name = '龙骨'
UNION ALL SELECT id, 1, 0.17, 0, DATE_SUB(NOW(), INTERVAL 60 DAY) FROM medicine_info WHERE name = '龙骨'
UNION ALL SELECT id, 1, 0.17, 0, DATE_SUB(NOW(), INTERVAL 30 DAY) FROM medicine_info WHERE name = '龙骨'
UNION ALL SELECT id, 1, 0.18, 0, NOW() FROM medicine_info WHERE name = '龙骨';
