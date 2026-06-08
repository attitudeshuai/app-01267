-- 一次性迁移：用户反馈、操作日志、商户通知类型扩展
-- 适用于已有数据库，执行前请备份

-- 1. 用户反馈表
CREATE TABLE IF NOT EXISTS feedback_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    purchaser_id BIGINT COMMENT '采购商ID',
    type TINYINT DEFAULT 1 COMMENT '1-功能建议 2-问题反馈 3-投诉 4-其他',
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    contact VARCHAR(100) COMMENT '联系方式',
    status TINYINT DEFAULT 0 COMMENT '0-待处理 1-已处理 2-已关闭',
    reply TEXT COMMENT '管理员回复',
    reply_time DATETIME COMMENT '回复时间',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_purchaser (purchaser_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户反馈表';

-- 2. 操作日志表
CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    operator_type TINYINT DEFAULT 1 COMMENT '1-管理员 2-商户 3-采购商',
    operator_id BIGINT COMMENT '操作人ID',
    operator_name VARCHAR(100) COMMENT '操作人名称',
    module VARCHAR(50) COMMENT '模块',
    action VARCHAR(100) COMMENT '操作',
    target_type VARCHAR(50) COMMENT '目标类型',
    target_id BIGINT COMMENT '目标ID',
    detail TEXT COMMENT '操作详情JSON',
    ip VARCHAR(50) COMMENT 'IP地址',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_operator (operator_type, operator_id),
    INDEX idx_module (module),
    INDEX idx_created (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- 3. 商户通知类型扩展（仅修改注释，不影响现有数据）
ALTER TABLE merchant_notification MODIFY COLUMN type TINYINT DEFAULT 1 COMMENT '1-库存预警 2-价格异常 3-新订单 4-药材审核 5-系统公告';
