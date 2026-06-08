-- 用户反馈表
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
