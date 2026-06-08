-- 商户通知类型扩展：1-库存预警 2-价格异常 3-新订单 4-药材审核 5-系统公告
ALTER TABLE merchant_notification MODIFY COLUMN type TINYINT DEFAULT 1 COMMENT '1-库存预警 2-价格异常 3-新订单 4-药材审核 5-系统公告';
