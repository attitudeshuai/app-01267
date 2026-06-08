-- ============================================
-- 药材销售管理系统 数据库初始化脚本
-- ============================================
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

CREATE DATABASE IF NOT EXISTS medicine_sales DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE medicine_sales;

-- 管理员表
CREATE TABLE IF NOT EXISTS admin_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    avatar VARCHAR(255),
    phone VARCHAR(20),
    email VARCHAR(100),
    status TINYINT DEFAULT 1 COMMENT '1-启用 0-禁用',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员表';

-- 商户信息表
CREATE TABLE IF NOT EXISTS merchant_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    company_name VARCHAR(100) NOT NULL,
    license_no VARCHAR(100),
    license_image VARCHAR(255),
    qualification_image VARCHAR(255),
    contact_person VARCHAR(50),
    phone VARCHAR(20),
    email VARCHAR(100),
    status TINYINT DEFAULT 0 COMMENT '0-待审核 1-已通过 2-已拒绝 3-已禁用',
    score DECIMAL(3,1) DEFAULT 5.0 COMMENT '动态评分',
    avatar VARCHAR(255),
    description TEXT,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户信息表';

-- 采购商信息表
CREATE TABLE IF NOT EXISTS purchaser_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    phone VARCHAR(20),
    email VARCHAR(100),
    avatar VARCHAR(255),
    status TINYINT DEFAULT 1 COMMENT '1-启用 0-禁用',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购商信息表';

-- 药材分类表
CREATE TABLE IF NOT EXISTS medicine_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    parent_id BIGINT DEFAULT 0 COMMENT '父分类ID，0为顶级',
    sort_order INT DEFAULT 0,
    icon VARCHAR(255),
    status TINYINT DEFAULT 1 COMMENT '1-启用 0-禁用',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药材分类表';

-- 药材信息表
CREATE TABLE IF NOT EXISTS medicine_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    category_id BIGINT NOT NULL,
    origin VARCHAR(100) COMMENT '产地',
    quality_grade VARCHAR(20) COMMENT '质量等级',
    specification VARCHAR(100) COMMENT '规格',
    unit VARCHAR(20) DEFAULT '克' COMMENT '单位',
    price DECIMAL(10,2) NOT NULL,
    merchant_id BIGINT NOT NULL,
    trace_code VARCHAR(100) COMMENT '质量追溯码',
    image VARCHAR(255),
    images VARCHAR(1000) COMMENT '多图，逗号分隔',
    description TEXT,
    trace_info TEXT COMMENT '追溯信息JSON',
    status TINYINT DEFAULT 0 COMMENT '0-待审核 1-已上架 2-已下架 3-已拒绝',
    sales_count INT DEFAULT 0,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category_id),
    INDEX idx_merchant (merchant_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药材信息表';

-- 订单表
CREATE TABLE IF NOT EXISTS order_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(50) NOT NULL UNIQUE,
    purchaser_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    order_status TINYINT DEFAULT 0 COMMENT '0-待支付 1-已支付待发货 2-已发货 3-已收货 4-已完成 5-已取消',
    pay_time DATETIME,
    ship_time DATETIME,
    receive_time DATETIME,
    tracking_no VARCHAR(100) COMMENT '物流单号',
    receiver_name VARCHAR(50),
    receiver_phone VARCHAR(20),
    receiver_address VARCHAR(500),
    remark VARCHAR(500),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_purchaser (purchaser_id),
    INDEX idx_merchant (merchant_id),
    INDEX idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- 订单明细表
CREATE TABLE IF NOT EXISTS order_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    medicine_id BIGINT NOT NULL,
    medicine_name VARCHAR(100),
    medicine_image VARCHAR(255),
    quantity INT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    rating TINYINT COMMENT '评分1-5',
    review TEXT COMMENT '评价内容',
    review_time DATETIME,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单明细表';

-- 库存表
CREATE TABLE IF NOT EXISTS inventory_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    medicine_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    warning_threshold INT DEFAULT 10 COMMENT '预警阈值',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_medicine_merchant (medicine_id, merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存表';

-- 价格记录表
CREATE TABLE IF NOT EXISTS price_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    medicine_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    is_abnormal TINYINT DEFAULT 0 COMMENT '0-正常 1-异常',
    record_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_medicine (medicine_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='价格记录表';

-- 推荐记录表
CREATE TABLE IF NOT EXISTS recommend_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    purchaser_id BIGINT NOT NULL,
    medicine_id BIGINT NOT NULL,
    recommend_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_clicked TINYINT DEFAULT 0,
    is_ordered TINYINT DEFAULT 0,
    INDEX idx_purchaser (purchaser_id),
    INDEX idx_purchaser_medicine (purchaser_id, medicine_id),
    INDEX idx_recommend_time (recommend_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='推荐记录表';

-- 购物车表
CREATE TABLE IF NOT EXISTS cart_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    purchaser_id BIGINT NOT NULL,
    medicine_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_purchaser (purchaser_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车表';

-- 收货地址表
CREATE TABLE IF NOT EXISTS address_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    purchaser_id BIGINT NOT NULL,
    receiver_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    province VARCHAR(50),
    city VARCHAR(50),
    district VARCHAR(50),
    detail_address VARCHAR(255) NOT NULL,
    is_default TINYINT DEFAULT 0,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_purchaser (purchaser_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收货地址表';

-- 收藏表
CREATE TABLE IF NOT EXISTS collection_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    purchaser_id BIGINT NOT NULL,
    medicine_id BIGINT NOT NULL,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_purchaser_medicine (purchaser_id, medicine_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

-- 轮播图表
CREATE TABLE IF NOT EXISTS banner_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100),
    image VARCHAR(255) NOT NULL,
    link_url VARCHAR(255),
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1 COMMENT '1-显示 0-隐藏',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='轮播图表';

-- 系统公告表
CREATE TABLE IF NOT EXISTS announcement_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    publisher_id BIGINT,
    status TINYINT DEFAULT 1 COMMENT '1-发布 0-草稿',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统公告表';

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

-- 操作日志表
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

-- 商户消息通知表（库存预警、订单、审核、公告等）
CREATE TABLE IF NOT EXISTS merchant_notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    type TINYINT DEFAULT 1 COMMENT '1-库存预警 2-价格异常 3-新订单 4-药材审核 5-系统公告',
    title VARCHAR(200) NOT NULL,
    content TEXT,
    extra_data TEXT COMMENT '扩展数据JSON，如药材ID、当前库存等',
    is_read TINYINT DEFAULT 0 COMMENT '0-未读 1-已读',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_merchant (merchant_id),
    INDEX idx_read (is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户消息通知表';

-- 系统配置表
CREATE TABLE IF NOT EXISTS system_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value VARCHAR(500),
    description VARCHAR(200),
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- ============================================
-- 初始数据
-- ============================================

-- 默认管理员由 DataInitializer 在应用启动时自动创建 (admin / admin123)

-- 药材分类
INSERT INTO medicine_category (name, parent_id, sort_order, status) VALUES
('根茎类', 0, 1, 1),
('花类', 0, 2, 1),
('叶类', 0, 3, 1),
('果实类', 0, 4, 1),
('矿物类', 0, 5, 1),
('动物类', 0, 6, 1),
('全草类', 0, 7, 1),
('树皮类', 0, 8, 1);

-- 系统配置
INSERT INTO system_config (config_key, config_value, description) VALUES
('trade_fee_rate', '0.02', '交易手续费率'),
('inventory_warning_threshold', '50', '默认库存预警阈值'),
('default_stock_quantity', '500', '新增药材默认库存数量'),
('price_abnormal_rate', '0.3', '价格异常波动率(30%)'),
('site_name', '中药材销售平台', '站点名称'),
('site_description', '专业的中药材交易平台', '站点描述'),
('recommend_similar_count', '10', '协同过滤相似用户数量上限'),
('recommend_item_count', '10', '推荐结果数量'),
('recommend_cache_hours', '1', '推荐缓存有效期(小时)');

-- 默认轮播图
INSERT INTO banner_info (title, image, link_url, sort_order, status, created_time) VALUES
('欢迎来到中药材销售平台', '/images/banner1.jpg', '/', 1, 1, NOW()),
('品质保障 道地药材', '/images/banner2.jpg', '/medicines', 2, 1, NOW()),
('商户入驻 共创未来', '/images/banner3.jpg', '/register/merchant', 3, 1, NOW());

-- 默认商户 (密码: merchant123, BCrypt加密)
INSERT INTO merchant_info (username, password, company_name, license_no, contact_person, phone, email, status, score, description, created_time) VALUES
('tongrentang', '$2a$10$X/uMNuiis.lDZ9HYpYHX7u6p7T3H7eMaJpDpCKJyQkQYFJBCHGKGe', '北京同仁堂药业', 'BJ20230001', '李经理', '13800138001', 'tongrentang@example.com', 1, 4.9, '百年老字号，专注道地药材', NOW()),
('huqingyutang', '$2a$10$X/uMNuiis.lDZ9HYpYHX7u6p7T3H7eMaJpDpCKJyQkQYFJBCHGKGe', '杭州胡庆余堂', 'HZ20230002', '王经理', '13800138002', 'huqingyutang@example.com', 1, 4.8, '江南药王，传承古方', NOW()),
('yunnanbaiyao', '$2a$10$X/uMNuiis.lDZ9HYpYHX7u6p7T3H7eMaJpDpCKJyQkQYFJBCHGKGe', '云南白药集团', 'YN20230003', '张经理', '13800138003', 'yunnanbaiyao@example.com', 1, 4.7, '云南道地药材直供', NOW());

-- 默认采购商 (密码: purchaser123, BCrypt加密)
INSERT INTO purchaser_info (username, password, nickname, phone, email, status, created_time) VALUES
('buyer001', '$2a$10$rDkPvvAFV8kqwvKJzwlQv.8QvSc1rVEKx9F.oH.1LbBskvKhCLI5O', '仁心药房', '13900139001', 'buyer001@example.com', 1, NOW()),
('buyer002', '$2a$10$rDkPvvAFV8kqwvKJzwlQv.8QvSc1rVEKx9F.oH.1LbBskvKhCLI5O', '济世堂连锁', '13900139002', 'buyer002@example.com', 1, NOW()),
('buyer003', '$2a$10$rDkPvvAFV8kqwvKJzwlQv.8QvSc1rVEKx9F.oH.1LbBskvKhCLI5O', '康泰药业', '13900139003', 'buyer003@example.com', 1, NOW());

-- 默认药材数据（真实中药材）
INSERT INTO medicine_info (name, category_id, origin, quality_grade, specification, unit, price, merchant_id, description, status, sales_count, created_time) VALUES
-- 根茎类 (category_id=1)
('人参', 1, '吉林长白山', '特级', '5-8年生整支', '克', 2.80, 1, '长白山野山参，补气固脱、健脾益肺、宁心益智。适用于体虚欲脱、肢冷脉微等症。', 1, 156, NOW()),
('黄芪', 1, '甘肃岷县', '一级', '精选切片', '克', 0.35, 1, '甘肃道地黄芪，补气升阳、固表止汗、利水消肿。适用于气虚乏力、食少便溏。', 1, 328, NOW()),
('当归', 1, '甘肃岷县', '特级', '整支无硫', '克', 0.45, 1, '岷县当归王，补血活血、调经止痛、润肠通便。适用于血虚萎黄、眩晕心悸。', 1, 267, NOW()),
('三七', 1, '云南文山', '20头', '春七优选', '克', 1.20, 3, '文山三七，散瘀止血、消肿定痛。适用于跌打损伤、瘀血肿痛。', 1, 423, NOW()),
('白术', 1, '浙江磐安', '一级', '生晒切片', '克', 0.28, 2, '浙江白术，健脾益气、燥湿利水、止汗安胎。适用于脾虚食少、腹胀泄泻。', 1, 189, NOW()),
('党参', 1, '甘肃渭源', '特级', '整条精选', '克', 0.38, 1, '甘肃党参，补中益气、健脾益肺。适用于脾肺虚弱、气短心悸、食少便溏。', 1, 234, NOW()),
('熟地黄', 1, '河南焦作', '特级', '九蒸九晒', '克', 0.32, 2, '怀庆熟地，滋阴补血、益精填髓。适用于肝肾阴虚、腰膝酸软。', 1, 178, NOW()),
('川芎', 1, '四川都江堰', '一级', '精选切片', '克', 0.25, 3, '川产川芎，活血行气、祛风止痛。适用于月经不调、头痛、风湿痹痛。', 1, 256, NOW()),
('黄连', 1, '四川石柱', '特级', '野生品种', '克', 0.85, 3, '四川黄连，清热燥湿、泻火解毒。适用于湿热痞满、泻痢、黄疸。', 1, 134, NOW()),
('甘草', 1, '内蒙古', '一级', '切片', '克', 0.15, 1, '内蒙甘草，补脾益气、清热解毒、调和诸药。为百药之长。', 1, 567, NOW()),
-- 花类 (category_id=2)
('金银花', 2, '山东平邑', '特级', '密银花', '克', 0.55, 1, '山东金银花，清热解毒、疏散风热。适用于外感风热、温病发热。', 1, 445, NOW()),
('菊花', 2, '浙江桐乡', '贡菊级', '胎菊王', '克', 0.48, 2, '杭白菊贡菊，散风清热、平肝明目。适用于风热感冒、头痛眩晕、目赤肿痛。', 1, 389, NOW()),
('红花', 2, '新疆', '一级', '精选花丝', '克', 0.42, 3, '新疆红花，活血通经、散瘀止痛。适用于经闭、痛经、跌打损伤。', 1, 234, NOW()),
('玫瑰花', 2, '山东平阴', '特级', '无硫干燥', '克', 0.38, 2, '平阴玫瑰，行气解郁、和血散瘀。适用于肝胃气痛、月经不调。', 1, 312, NOW()),
('槐花', 2, '河南', '一级', '生晒', '克', 0.12, 1, '河南槐花，凉血止血、清肝泻火。适用于便血、痔血、尿血。', 1, 167, NOW()),
-- 叶类 (category_id=3)
('艾叶', 3, '湖北蕲春', '特级', '三年陈艾', '克', 0.08, 2, '蕲春艾叶，温经止血、散寒止痛、祛湿止痒。适用于虚寒出血、月经不调。', 1, 523, NOW()),
('桑叶', 3, '浙江湖州', '一级', '霜桑叶', '克', 0.06, 2, '浙江桑叶，疏散风热、清肺润燥、清肝明目。适用于风热感冒、肺热燥咳。', 1, 234, NOW()),
('薄荷', 3, '江苏', '一级', '精选叶片', '克', 0.12, 1, '江苏薄荷，疏散风热、清利头目、利咽透疹。适用于风热感冒、头痛目赤。', 1, 345, NOW()),
('紫苏叶', 3, '广东', '一级', '鲜叶干燥', '克', 0.10, 3, '紫苏叶，解表散寒、行气和胃。适用于风寒感冒、咳嗽气喘、胸腹胀满。', 1, 178, NOW()),
-- 果实类 (category_id=4)
('枸杞子', 4, '宁夏中宁', '特级', '大粒免洗', '克', 0.28, 1, '宁夏枸杞，滋补肝肾、益精明目。适用于虚劳精亏、腰膝酸软、眩晕耳鸣。', 1, 678, NOW()),
('大枣', 4, '新疆若羌', '特级', '灰枣王', '克', 0.06, 3, '若羌灰枣，补中益气、养血安神。适用于脾虚食少、乏力便溏、妇人脏躁。', 1, 456, NOW()),
('山楂', 4, '山东', '一级', '干片', '克', 0.08, 2, '山东山楂，消食健胃、行气散瘀。适用于肉食积滞、胃脘胀满、泻痢腹痛。', 1, 345, NOW()),
('五味子', 4, '辽宁', '一级', '北五味子', '克', 0.35, 1, '辽宁五味子，收敛固涩、益气生津、补肾宁心。适用于久嗽虚喘、自汗盗汗。', 1, 234, NOW()),
('决明子', 4, '安徽', '一级', '炒制', '克', 0.05, 2, '决明子，清热明目、润肠通便。适用于目赤肿痛、头痛眩晕、大便秘结。', 1, 567, NOW()),
('陈皮', 4, '广东新会', '十年陈', '特级老陈皮', '克', 0.85, 3, '新会陈皮，理气健脾、燥湿化痰。适用于脘腹胀满、食少吐泻、咳嗽痰多。', 1, 289, NOW()),
-- 矿物类 (category_id=5)
('龙骨', 5, '河南', '一级', '煅制', '克', 0.18, 1, '龙骨，镇惊安神、平肝潜阳、收敛固涩。适用于心悸失眠、惊痫癫狂。', 1, 123, NOW()),
('牡蛎', 5, '山东', '一级', '煅牡蛎', '克', 0.08, 2, '牡蛎，重镇安神、潜阳补阴、软坚散结。适用于惊悸失眠、眩晕耳鸣。', 1, 156, NOW()),
('石膏', 5, '湖北', '一级', '生石膏', '克', 0.03, 1, '石膏，清热泻火、除烦止渴。适用于外感热病、高热烦渴、肺热喘咳。', 1, 234, NOW()),
-- 动物类 (category_id=6)
('阿胶', 6, '山东东阿', '特级', '铁盒装250g', '克', 3.50, 1, '东阿阿胶，补血滋阴、润燥止血。适用于血虚萎黄、眩晕心悸、肌痿无力。', 1, 345, NOW()),
('蜂蜜', 6, '云南', '野生土蜂蜜', '原蜜', '克', 0.08, 3, '云南野生蜂蜜，补中润燥、止痛解毒。适用于脘腹虚痛、肺燥干咳。', 1, 567, NOW()),
-- 全草类 (category_id=7)
('蒲公英', 7, '河北', '一级', '全草', '克', 0.06, 2, '蒲公英，清热解毒、消肿散结、利尿通淋。适用于疔疮肿毒、乳痈、咽痛。', 1, 234, NOW()),
('鱼腥草', 7, '四川', '一级', '干制全草', '克', 0.08, 3, '鱼腥草，清热解毒、消痈排脓、利尿通淋。适用于肺痈吐脓、热痢、热淋。', 1, 189, NOW()),
('益母草', 7, '河南', '一级', '全草', '克', 0.06, 1, '益母草，活血调经、利尿消肿、清热解毒。适用于月经不调、痛经、恶露不尽。', 1, 345, NOW()),
-- 树皮类 (category_id=8)
('杜仲', 8, '四川', '特级', '盐制切片', '克', 0.25, 3, '川杜仲，补肝肾、强筋骨、安胎。适用于腰膝酸痛、筋骨无力、妊娠漏血。', 1, 234, NOW()),
('肉桂', 8, '广西', '一级', '桂皮卷', '克', 0.15, 2, '广西肉桂，补火助阳、引火归源、散寒止痛。适用于阳痿宫冷、腰膝冷痛。', 1, 178, NOW()),
('黄柏', 8, '四川', '一级', '切片', '克', 0.12, 1, '川黄柏，清热燥湿、泻火除蒸、解毒疗疮。适用于湿热泻痢、黄疸尿赤。', 1, 145, NOW());

-- 库存数据
INSERT INTO inventory_info (medicine_id, merchant_id, stock_quantity, warning_threshold, created_time) VALUES
(1, 1, 500, 50, NOW()),
(2, 1, 2000, 100, NOW()),
(3, 1, 1500, 100, NOW()),
(4, 3, 800, 50, NOW()),
(5, 2, 1200, 80, NOW()),
(6, 1, 1000, 60, NOW()),
(7, 2, 800, 50, NOW()),
(8, 3, 1500, 100, NOW()),
(9, 3, 25, 30, NOW()),
(10, 1, 3000, 200, NOW()),
(11, 1, 600, 50, NOW()),
(12, 2, 800, 60, NOW()),
(13, 3, 500, 40, NOW()),
(14, 2, 700, 50, NOW()),
(15, 1, 1000, 80, NOW()),
(16, 2, 2000, 150, NOW()),
(17, 2, 1500, 100, NOW()),
(18, 1, 2000, 150, NOW()),
(19, 3, 1200, 80, NOW()),
(20, 1, 2500, 200, NOW()),
(21, 3, 3000, 200, NOW()),
(22, 2, 1800, 120, NOW()),
(23, 1, 1000, 80, NOW()),
(24, 2, 2000, 150, NOW()),
(25, 3, 600, 50, NOW()),
(26, 1, 800, 60, NOW()),
(27, 2, 1200, 80, NOW()),
(28, 1, 1500, 100, NOW()),
(29, 1, 20, 30, NOW()),
(30, 3, 3000, 200, NOW()),
(31, 2, 1500, 100, NOW()),
(32, 3, 1200, 80, NOW()),
(33, 1, 1800, 120, NOW()),
(34, 3, 1000, 80, NOW()),
(35, 2, 1200, 80, NOW()),
(36, 1, 45, 60, NOW());

-- 收货地址
INSERT INTO address_info (purchaser_id, receiver_name, phone, province, city, district, detail_address, is_default, created_time) VALUES
(1, '张三', '13900139001', '北京市', '朝阳区', '建国路', '建国路88号仁心大厦1层', 1, NOW()),
(1, '张三', '13900139001', '北京市', '海淀区', '中关村', '中关村科技园区8号楼', 0, NOW()),
(2, '李四', '13900139002', '上海市', '浦东新区', '陆家嘴', '陆家嘴金融中心12层', 1, NOW()),
(3, '王五', '13900139003', '广东省', '广州市', '天河区', '天河路385号太古汇', 1, NOW());

-- 订单数据
INSERT INTO order_info (order_no, purchaser_id, merchant_id, total_amount, order_status, receiver_name, receiver_phone, receiver_address, remark, created_time, pay_time) VALUES
('ORD20260201001', 1, 1, 280.00, 4, '张三', '13900139001', '北京市朝阳区建国路88号仁心大厦1层', '请尽快发货', DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY)),
('ORD20260205002', 1, 2, 156.50, 4, '张三', '13900139001', '北京市朝阳区建国路88号仁心大厦1层', NULL, DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY)),
('ORD20260210003', 2, 1, 520.00, 3, '李四', '13900139002', '上海市浦东新区陆家嘴金融中心12层', '需要发票', DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY)),
('ORD20260215004', 2, 3, 345.60, 2, '李四', '13900139002', '上海市浦东新区陆家嘴金融中心12层', NULL, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
('ORD20260220005', 3, 1, 168.00, 1, '王五', '13900139003', '广东省广州市天河区天河路385号太古汇', '送货上门', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
('ORD20260225006', 3, 2, 89.50, 0, '王五', '13900139003', '广东省广州市天河区天河路385号太古汇', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), NULL),
('ORD20260226007', 1, 3, 456.00, 1, '张三', '13900139001', '北京市海淀区中关村科技园区8号楼', NULL, NOW(), NOW());

-- 订单明细
INSERT INTO order_detail (order_id, medicine_id, medicine_name, quantity, price, subtotal, rating, review, review_time, created_time) VALUES
(1, 1, '人参', 100, 2.80, 280.00, 5, '品质很好，是正宗的长白山人参', DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY)),
(2, 12, '菊花', 200, 0.48, 96.00, 5, '菊花很香，泡茶很好喝', DATE_SUB(NOW(), INTERVAL 13 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY)),
(2, 17, '桑叶', 100, 0.06, 6.00, 4, '质量不错', DATE_SUB(NOW(), INTERVAL 13 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY)),
(2, 18, '薄荷', 450, 0.12, 54.50, 5, '薄荷味道很清新', DATE_SUB(NOW(), INTERVAL 13 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY)),
(3, 2, '黄芪', 500, 0.35, 175.00, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(3, 3, '当归', 500, 0.45, 225.00, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(3, 10, '甘草', 800, 0.15, 120.00, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(4, 4, '三七', 200, 1.20, 240.00, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(4, 8, '川芎', 422, 0.25, 105.60, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(5, 20, '枸杞子', 600, 0.28, 168.00, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(6, 14, '玫瑰花', 200, 0.38, 76.00, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(6, 15, '槐花', 100, 0.12, 12.00, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(7, 25, '陈皮', 300, 0.85, 255.00, NULL, NULL, NULL, NOW()),
(7, 34, '杜仲', 500, 0.25, 125.00, NULL, NULL, NULL, NOW()),
(7, 9, '黄连', 90, 0.85, 76.00, NULL, NULL, NULL, NOW());

-- 价格记录（用于价格分析，每个药品4条历史记录）
INSERT INTO price_record (medicine_id, merchant_id, price, is_abnormal, record_time) VALUES
(1, 1, 2.50, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (1, 1, 2.60, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (1, 1, 2.70, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (1, 1, 2.80, 0, NOW()),
(2, 1, 0.30, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (2, 1, 0.32, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (2, 1, 0.33, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (2, 1, 0.35, 0, NOW()),
(3, 1, 0.40, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (3, 1, 0.42, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (3, 1, 0.44, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (3, 1, 0.45, 0, NOW()),
(4, 3, 0.95, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (4, 3, 1.05, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (4, 3, 1.10, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (4, 3, 1.20, 0, NOW()),
(5, 2, 0.25, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (5, 2, 0.26, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (5, 2, 0.27, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (5, 2, 0.28, 0, NOW()),
(6, 1, 0.34, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (6, 1, 0.36, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (6, 1, 0.37, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (6, 1, 0.38, 0, NOW()),
(7, 2, 0.29, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (7, 2, 0.30, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (7, 2, 0.31, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (7, 2, 0.32, 0, NOW()),
(8, 3, 0.22, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (8, 3, 0.23, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (8, 3, 0.24, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (8, 3, 0.25, 0, NOW()),
(9, 3, 0.76, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (9, 3, 0.80, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (9, 3, 0.83, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (9, 3, 0.85, 0, NOW()),
(10, 1, 0.13, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (10, 1, 0.14, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (10, 1, 0.14, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (10, 1, 0.15, 0, NOW()),
(11, 1, 0.49, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (11, 1, 0.52, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (11, 1, 0.54, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (11, 1, 0.55, 0, NOW()),
(12, 2, 0.43, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (12, 2, 0.45, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (12, 2, 0.47, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (12, 2, 0.48, 0, NOW()),
(13, 3, 0.38, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (13, 3, 0.40, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (13, 3, 0.41, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (13, 3, 0.42, 0, NOW()),
(14, 2, 0.34, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (14, 2, 0.36, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (14, 2, 0.37, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (14, 2, 0.38, 0, NOW()),
(15, 1, 0.11, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (15, 1, 0.11, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (15, 1, 0.12, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (15, 1, 0.12, 0, NOW()),
(16, 2, 0.07, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (16, 2, 0.07, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (16, 2, 0.08, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (16, 2, 0.08, 0, NOW()),
(17, 2, 0.05, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (17, 2, 0.05, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (17, 2, 0.06, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (17, 2, 0.06, 0, NOW()),
(18, 1, 0.11, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (18, 1, 0.11, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (18, 1, 0.12, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (18, 1, 0.12, 0, NOW()),
(19, 3, 0.09, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (19, 3, 0.09, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (19, 3, 0.10, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (19, 3, 0.10, 0, NOW()),
(20, 1, 0.22, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (20, 1, 0.24, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (20, 1, 0.26, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (20, 1, 0.28, 0, NOW()),
(21, 3, 0.05, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (21, 3, 0.05, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (21, 3, 0.06, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (21, 3, 0.06, 0, NOW()),
(22, 2, 0.07, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (22, 2, 0.07, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (22, 2, 0.08, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (22, 2, 0.08, 0, NOW()),
(23, 1, 0.25, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (23, 1, 0.26, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (23, 1, 0.27, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (23, 1, 0.28, 0, NOW()),
(24, 2, 0.31, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (24, 2, 0.33, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (24, 2, 0.34, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (24, 2, 0.35, 0, NOW()),
(25, 3, 0.76, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (25, 3, 0.80, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (25, 3, 0.83, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (25, 3, 0.85, 0, NOW()),
(26, 1, 0.04, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (26, 1, 0.04, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (26, 1, 0.05, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (26, 1, 0.05, 0, NOW()),
(27, 2, 0.07, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (27, 2, 0.07, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (27, 2, 0.08, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (27, 2, 0.08, 0, NOW()),
(28, 1, 0.16, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (28, 1, 0.17, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (28, 1, 0.17, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (28, 1, 0.18, 0, NOW()),
(29, 1, 0.07, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (29, 1, 0.07, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (29, 1, 0.08, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (29, 1, 0.08, 0, NOW()),
(30, 3, 0.07, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (30, 3, 0.07, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (30, 3, 0.08, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (30, 3, 0.08, 0, NOW()),
(31, 2, 0.13, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (31, 2, 0.14, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (31, 2, 0.14, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (31, 2, 0.15, 0, NOW()),
(32, 3, 0.22, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (32, 3, 0.23, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (32, 3, 0.24, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (32, 3, 0.25, 0, NOW()),
(33, 1, 0.05, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (33, 1, 0.05, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (33, 1, 0.06, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (33, 1, 0.06, 0, NOW()),
(34, 3, 0.22, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (34, 3, 0.23, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (34, 3, 0.24, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (34, 3, 0.25, 0, NOW()),
(35, 2, 0.11, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (35, 2, 0.11, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (35, 2, 0.12, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (35, 2, 0.12, 0, NOW()),
(36, 1, 0.11, 0, DATE_SUB(NOW(), INTERVAL 90 DAY)), (36, 1, 0.11, 0, DATE_SUB(NOW(), INTERVAL 60 DAY)), (36, 1, 0.12, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (36, 1, 0.12, 0, NOW());

-- 系统公告
INSERT INTO announcement_info (title, content, publisher_id, status, created_time) VALUES
('关于平台升级维护的通知', '尊敬的用户，为提升服务质量，本平台将于2026年3月1日凌晨2:00-6:00进行系统升级维护，届时部分功能可能暂时无法使用，请您提前做好准备，感谢您的理解与支持！', 1, 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),
('春季药材促销活动开始啦', '即日起至3月31日，全场药材享受95折优惠，部分精选药材低至8折。活动期间新用户注册即送50元优惠券，老用户推荐新用户双方各得30元优惠券。快来选购吧！', 1, 1, DATE_SUB(NOW(), INTERVAL 7 DAY)),
('新增云南白药集团入驻', '热烈欢迎云南白药集团正式入驻本平台！云南白药作为中国知名药企，将为大家带来优质的云南道地药材，包括三七、天麻、石斛等特色品种。敬请期待！', 1, 1, DATE_SUB(NOW(), INTERVAL 14 DAY)),
('关于规范药材质量追溯的说明', '为保障药材质量安全，本平台已全面启用质量追溯系统。所有上架药材均需提供产地证明、质检报告等资质文件，消费者可通过药材详情页查看追溯信息。', 1, 1, DATE_SUB(NOW(), INTERVAL 30 DAY));
