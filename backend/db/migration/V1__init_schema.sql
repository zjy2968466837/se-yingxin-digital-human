-- 数据建模文档（数据七要素）落地脚本
-- 规范 §5.3：集中式迁移，统一字符集 utf8mb4，禁止物理外键

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- auth-service
-- ============================================================
CREATE TABLE IF NOT EXISTS auth_user (
    id              BIGINT PRIMARY KEY COMMENT '雪花 ID',
    student_no      VARCHAR(32)  NULL COMMENT '学号',
    openid          VARCHAR(64)  NULL COMMENT '微信 openid',
    unionid         VARCHAR(64)  NULL COMMENT '微信 unionid',
    real_name       VARCHAR(32)  NULL COMMENT '姓名',
    phone           VARCHAR(20)  NULL COMMENT '手机号（加密存储）',
    password_hash   VARCHAR(100) NULL COMMENT 'BCrypt 密码哈希',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '1 正常 / 2 禁用 / 3 未激活',
    last_login_time DATETIME(3)  NULL COMMENT '最近登录时间',
    create_time     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by       BIGINT       NULL,
    update_by       BIGINT       NULL,
    deleted         TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY uk_openid (openid),
    UNIQUE KEY uk_student_no (student_no),
    KEY idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户账号';

CREATE TABLE IF NOT EXISTS auth_identity (
    id              BIGINT PRIMARY KEY COMMENT '雪花 ID',
    user_id         BIGINT       NOT NULL COMMENT '用户 ID',
    edu_level       TINYINT      NOT NULL DEFAULT 1 COMMENT '1 本科 / 2 研究生 / 3 其他',
    college         VARCHAR(64)  NULL COMMENT '学院',
    major           VARCHAR(64)  NULL COMMENT '专业',
    class_name      VARCHAR(64)  NULL COMMENT '班级',
    dorm_building   VARCHAR(32)  NULL COMMENT '宿舍楼栋',
    dorm_room       VARCHAR(32)  NULL COMMENT '宿舍房间',
    batch_id        BIGINT       NOT NULL COMMENT '报到批次',
    admission_type  TINYINT      NULL DEFAULT 1 COMMENT '1 统招 / 2 专项 / 3 其他',
    source          TINYINT      NOT NULL DEFAULT 1 COMMENT '1 学校身份源 / 2 模拟数据',
    create_time     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by       BIGINT       NULL,
    update_by       BIGINT       NULL,
    deleted         TINYINT(1)   NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_id (user_id),
    KEY idx_college_batch (college, batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='身份档案';

CREATE TABLE IF NOT EXISTS auth_push_record (
    id          BIGINT PRIMARY KEY COMMENT '雪花 ID',
    user_id     BIGINT       NOT NULL COMMENT '接收人',
    channel     TINYINT      NOT NULL DEFAULT 1 COMMENT '1 站内信 / 2 公众号模板消息',
    title       VARCHAR(64)  NOT NULL COMMENT '标题',
    content     VARCHAR(512) NOT NULL COMMENT '内容',
    biz_type    TINYINT      NULL COMMENT '关联业务类型',
    biz_id      BIGINT       NULL COMMENT '关联业务 ID',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1 待发 / 2 成功 / 3 失败',
    retry_count INT          NOT NULL DEFAULT 0 COMMENT '重试计数',
    fail_reason VARCHAR(255) NULL COMMENT '失败原因',
    create_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by   BIGINT       NULL,
    update_by   BIGINT       NULL,
    deleted     TINYINT(1)   NOT NULL DEFAULT 0,
    KEY idx_user_status (user_id, status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='待办推送记录';

CREATE TABLE IF NOT EXISTS auth_login_log (
    id          BIGINT PRIMARY KEY COMMENT '雪花 ID',
    user_id     BIGINT       NULL COMMENT '用户 ID，未匹配到为空',
    login_type  TINYINT      NOT NULL DEFAULT 1 COMMENT '1 学号密码 / 2 微信 / 3 后台账号',
    result      TINYINT      NOT NULL DEFAULT 1 COMMENT '1 成功 / 2 失败',
    fail_reason VARCHAR(64)  NULL COMMENT '失败原因',
    client_ip   VARCHAR(45)  NULL COMMENT '客户端 IP',
    user_agent  VARCHAR(255) NULL COMMENT '客户端标识',
    create_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by   BIGINT       NULL,
    update_by   BIGINT       NULL,
    deleted     TINYINT(1)   NOT NULL DEFAULT 0,
    KEY idx_user_time (user_id, create_time),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志';

-- ============================================================
-- checkin-service
-- ============================================================
CREATE TABLE IF NOT EXISTS checkin_step_template (
    id           BIGINT PRIMARY KEY COMMENT '雪花 ID',
    step_code    VARCHAR(32)  NOT NULL COMMENT '步骤编码',
    step_name    VARCHAR(64)  NOT NULL COMMENT '步骤名称',
    seq          INT          NOT NULL DEFAULT 0 COMMENT '顺序号',
    apply_scope  JSON         NULL COMMENT '适用人群',
    location     VARCHAR(128) NULL COMMENT '办理地点',
    office_hours VARCHAR(64)  NULL COMMENT '办公时间',
    contact      VARCHAR(64)  NULL COMMENT '联系人电话',
    enabled      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '启停开关',
    create_time  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by    BIGINT       NULL,
    update_by    BIGINT       NULL,
    deleted      TINYINT(1)   NOT NULL DEFAULT 0,
    UNIQUE KEY uk_step_code (step_code),
    KEY idx_seq (seq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报到步骤模板';

CREATE TABLE IF NOT EXISTS checkin_material_item (
    id          BIGINT PRIMARY KEY COMMENT '雪花 ID',
    step_code   VARCHAR(32)  NOT NULL COMMENT '归属步骤',
    name        VARCHAR(64)  NOT NULL COMMENT '材料名称',
    required    TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否必需',
    remark      VARCHAR(255) NULL COMMENT '补交说明',
    seq         INT          NOT NULL DEFAULT 0 COMMENT '展示顺序',
    create_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by   BIGINT       NULL,
    update_by   BIGINT       NULL,
    deleted     TINYINT(1)   NOT NULL DEFAULT 0,
    KEY idx_step (step_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='材料清单项';

CREATE TABLE IF NOT EXISTS checkin_progress (
    id              BIGINT PRIMARY KEY COMMENT '雪花 ID',
    user_id         BIGINT       NOT NULL COMMENT '学生',
    step_code       VARCHAR(32)  NOT NULL COMMENT '步骤',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '1 待办 / 2 已完成 / 3 不适用',
    checkin_time    DATETIME(3)  NULL COMMENT '打卡时间',
    checkin_channel TINYINT      NULL COMMENT '1 自助确认 / 2 现场扫码',
    operator_id     BIGINT       NULL COMMENT '代打卡经办人',
    remark          VARCHAR(255) NULL COMMENT '备注',
    create_time     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by       BIGINT       NULL,
    update_by       BIGINT       NULL,
    deleted         TINYINT(1)   NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_step (user_id, step_code, deleted),
    KEY idx_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报到进度记录';

CREATE TABLE IF NOT EXISTS checkin_certificate (
    id            BIGINT PRIMARY KEY COMMENT '雪花 ID',
    user_id       BIGINT       NOT NULL COMMENT '学生',
    cert_no       VARCHAR(32)  NOT NULL COMMENT '凭证编号',
    qr_token      VARCHAR(64)  NOT NULL COMMENT '二维码令牌',
    issued_time   DATETIME(3)  NOT NULL COMMENT '签发时间',
    expire_time   DATETIME(3)  NOT NULL COMMENT '过期时间',
    verify_status TINYINT      NOT NULL DEFAULT 1 COMMENT '1 未核销 / 2 已核销 / 3 已作废',
    verify_by     BIGINT       NULL COMMENT '核销人',
    verify_time   DATETIME(3)  NULL COMMENT '核销时间',
    create_time   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by     BIGINT       NULL,
    update_by     BIGINT       NULL,
    deleted       TINYINT(1)   NOT NULL DEFAULT 0,
    UNIQUE KEY uk_cert_no (cert_no),
    UNIQUE KEY uk_qr_token (qr_token),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报到凭证';

CREATE TABLE IF NOT EXISTS checkin_exception_guide (
    id              BIGINT PRIMARY KEY COMMENT '雪花 ID',
    exception_type  TINYINT      NOT NULL DEFAULT 1 COMMENT '1 未缴费 / 2 材料缺失 / 3 暂缓报到 / 4 改期 / 5 信息缺失',
    title           VARCHAR(64)  NOT NULL COMMENT '标题',
    guide_content   TEXT         NOT NULL COMMENT '办理指引',
    contact         VARCHAR(64)  NULL COMMENT '办理人/电话',
    poi_id          BIGINT       NULL COMMENT '办理地点',
    create_time     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by       BIGINT       NULL,
    update_by       BIGINT       NULL,
    deleted         TINYINT(1)   NOT NULL DEFAULT 0,
    KEY idx_type (exception_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='异常处理指引';

-- ============================================================
-- qa-service
-- ============================================================
CREATE TABLE IF NOT EXISTS qa_question (
    id           BIGINT PRIMARY KEY COMMENT '雪花 ID',
    question     VARCHAR(255)  NOT NULL COMMENT '标准问法',
    answer       MEDIUMTEXT    NOT NULL COMMENT '答案正文',
    answer_type  TINYINT       NOT NULL DEFAULT 1 COMMENT '1 纯文本 / 2 富媒体',
    category     VARCHAR(32)   NOT NULL COMMENT '知识域',
    tags         VARCHAR(255)  NULL COMMENT '标签',
    synonyms     TEXT          NULL COMMENT '同义词/口语化表达',
    media        JSON          NULL COMMENT '富媒体块定义',
    status       TINYINT       NOT NULL DEFAULT 1 COMMENT '1 已发布 / 2 草稿 / 3 停用',
    hit_count    BIGINT        NOT NULL DEFAULT 0 COMMENT '命中次数',
    version      INT           NOT NULL DEFAULT 1 COMMENT '内容版本号',
    create_time  DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time  DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by    BIGINT        NULL,
    update_by    BIGINT        NULL,
    deleted      TINYINT(1)    NOT NULL DEFAULT 0,
    KEY idx_category_status (category, status),
    FULLTEXT KEY idx_question (question, synonyms)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问答条目';

CREATE TABLE IF NOT EXISTS qa_session (
    id             BIGINT PRIMARY KEY COMMENT '雪花 ID',
    user_id        BIGINT       NULL COMMENT '用户 ID，匿名访问为空',
    channel        TINYINT      NOT NULL DEFAULT 1 COMMENT '1 小程序 / 2 大屏 / 3 H5',
    start_time     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '开始时间',
    end_time       DATETIME(3)  NULL COMMENT '结束时间',
    message_count  INT          NOT NULL DEFAULT 0 COMMENT '消息数',
    satisfied      TINYINT      NULL COMMENT '满意度',
    create_time    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by      BIGINT       NULL,
    update_by      BIGINT       NULL,
    deleted        TINYINT(1)   NOT NULL DEFAULT 0,
    KEY idx_user_time (user_id, start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问答会话';

CREATE TABLE IF NOT EXISTS qa_message (
    id            BIGINT PRIMARY KEY COMMENT '雪花 ID',
    session_id    BIGINT       NOT NULL COMMENT '会话 ID',
    role          TINYINT      NOT NULL DEFAULT 1 COMMENT '1 用户 / 2 数字人',
    content       TEXT         NOT NULL COMMENT '内容（加密存储）',
    content_type  TINYINT      NOT NULL DEFAULT 1 COMMENT '1 文本 / 2 富媒体',
    question_id   BIGINT       NULL COMMENT '命中问答库条目',
    confidence    DECIMAL(5,2) NULL COMMENT '置信度',
    llm_used      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否 LLM 兜底',
    create_time   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by     BIGINT       NULL,
    update_by     BIGINT       NULL,
    deleted       TINYINT(1)   NOT NULL DEFAULT 0,
    KEY idx_session (session_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问答消息';

CREATE TABLE IF NOT EXISTS qa_unanswered (
    id           BIGINT PRIMARY KEY COMMENT '雪花 ID',
    question     VARCHAR(255)  NOT NULL COMMENT '原始问题',
    session_id   BIGINT        NULL COMMENT '来源会话',
    user_id      BIGINT        NULL COMMENT '用户 ID',
    confidence   DECIMAL(5,2)  NULL COMMENT '最高置信度',
    status       TINYINT       NOT NULL DEFAULT 1 COMMENT '1 待处理 / 2 已转人工 / 3 已补录问答库 / 4 已忽略',
    handler_id   BIGINT        NULL COMMENT '处理人',
    handle_note  VARCHAR(255)  NULL COMMENT '处理说明',
    create_time  DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time  DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by    BIGINT        NULL,
    update_by    BIGINT        NULL,
    deleted      TINYINT(1)    NOT NULL DEFAULT 0,
    KEY idx_status_time (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='未解答记录';

CREATE TABLE IF NOT EXISTS qa_ask_log (
    id            BIGINT PRIMARY KEY COMMENT '雪花 ID',
    user_id       BIGINT       NULL COMMENT '用户 ID，匿名访问为空',
    question_hash VARCHAR(64)  NOT NULL COMMENT '问题指纹',
    hit_type      TINYINT      NOT NULL DEFAULT 1 COMMENT '1 缓存命中 / 2 问答库命中 / 3 LLM 兜底 / 4 未命中',
    cost_ms       INT          NOT NULL DEFAULT 0 COMMENT '响应耗时毫秒',
    session_id    BIGINT       NULL COMMENT '关联会话',
    create_time   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by     BIGINT       NULL,
    update_by     BIGINT       NULL,
    deleted       TINYINT(1)   NOT NULL DEFAULT 0,
    KEY idx_create_time (create_time),
    KEY idx_hit_type (hit_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提问日志';

-- ============================================================
-- navigation-service
-- ============================================================
CREATE TABLE IF NOT EXISTS nav_poi (
    id          BIGINT PRIMARY KEY COMMENT '雪花 ID',
    name        VARCHAR(64)   NOT NULL COMMENT '名称',
    alias       VARCHAR(255)  NULL COMMENT '别名',
    category    TINYINT       NOT NULL DEFAULT 5 COMMENT '1 报到点 / 2 宿舍 / 3 食堂 / 4 教学楼 / 5 服务设施 / 6 门 / 7 建筑',
    building_id BIGINT        NULL COMMENT '所属建筑',
    longitude   DECIMAL(10,7) NOT NULL COMMENT '经度',
    latitude    DECIMAL(10,7) NOT NULL COMMENT '纬度',
    floor_desc  VARCHAR(64)   NULL COMMENT '楼层分布',
    open_hours  VARCHAR(64)   NULL COMMENT '开放时间',
    intro       TEXT          NULL COMMENT '地标讲解',
    tags        VARCHAR(255)  NULL COMMENT '检索标签',
    create_time DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by   BIGINT        NULL,
    update_by   BIGINT        NULL,
    deleted     TINYINT(1)    NOT NULL DEFAULT 0,
    KEY idx_category (category),
    KEY idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='导航点位';

CREATE TABLE IF NOT EXISTS nav_node (
    id          BIGINT PRIMARY KEY COMMENT '雪花 ID',
    name        VARCHAR(64)   NULL COMMENT '节点名称',
    longitude   DECIMAL(10,7) NOT NULL COMMENT '经度',
    latitude    DECIMAL(10,7) NOT NULL COMMENT '纬度',
    poi_id      BIGINT        NULL COMMENT '关联点位',
    node_type   TINYINT       NOT NULL DEFAULT 1 COMMENT '1 路口 / 2 出入口 / 3 点位节点',
    create_time DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by   BIGINT        NULL,
    update_by   BIGINT        NULL,
    deleted     TINYINT(1)    NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='路网节点';

CREATE TABLE IF NOT EXISTS nav_edge (
    id            BIGINT PRIMARY KEY COMMENT '雪花 ID',
    from_node_id  BIGINT        NOT NULL COMMENT '起点节点',
    to_node_id    BIGINT        NOT NULL COMMENT '终点节点',
    distance_m    INT           NOT NULL DEFAULT 0 COMMENT '距离（米）',
    walk_seconds  INT           NOT NULL DEFAULT 0 COMMENT '步行秒数',
    bidirectional TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '是否双向',
    edge_type     TINYINT       NOT NULL DEFAULT 1 COMMENT '1 步行道 / 2 通勤车道',
    create_time   DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time   DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by     BIGINT        NULL,
    update_by     BIGINT        NULL,
    deleted       TINYINT(1)    NOT NULL DEFAULT 0,
    UNIQUE KEY uk_from_to (from_node_id, to_node_id),
    KEY idx_to (to_node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='路网边';

CREATE TABLE IF NOT EXISTS nav_favorite (
    id          BIGINT PRIMARY KEY COMMENT '雪花 ID',
    user_id     BIGINT       NOT NULL COMMENT '学生',
    poi_id      BIGINT       NOT NULL COMMENT '点位',
    seq         INT          NOT NULL DEFAULT 0 COMMENT '排序',
    create_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by   BIGINT       NULL,
    update_by   BIGINT       NULL,
    deleted     TINYINT(1)   NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_poi (user_id, poi_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='常用地点';

-- ============================================================
-- aid-service
-- ============================================================
CREATE TABLE IF NOT EXISTS aid_policy (
    id            BIGINT PRIMARY KEY COMMENT '雪花 ID',
    name          VARCHAR(128)  NOT NULL COMMENT '政策名称',
    category      TINYINT       NOT NULL DEFAULT 6 COMMENT '1 国家助学金 / 2 助学贷款 / 3 勤工助学 / 4 困难补助 / 5 缓交学费 / 6 其他',
    content       MEDIUMTEXT    NOT NULL COMMENT '政策正文',
    match_rule    JSON          NULL COMMENT '适用条件',
    material_list TEXT          NULL COMMENT '所需材料明细',
    contact       VARCHAR(64)   NULL COMMENT '咨询电话',
    location      VARCHAR(128)  NULL COMMENT '办理地点',
    online_entry  VARCHAR(255)  NULL COMMENT '在线办理入口',
    need_apply    TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '是否需要线上申请',
    status        TINYINT       NOT NULL DEFAULT 1 COMMENT '1 启用 / 2 停用',
    create_time   DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time   DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by     BIGINT        NULL,
    update_by     BIGINT        NULL,
    deleted       TINYINT(1)    NOT NULL DEFAULT 0,
    KEY idx_category_status (category, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资助政策';

CREATE TABLE IF NOT EXISTS aid_application (
    id            BIGINT PRIMARY KEY COMMENT '雪花 ID',
    user_id       BIGINT       NOT NULL COMMENT '申请学生',
    policy_id     BIGINT       NOT NULL COMMENT '申请的政策',
    batch_id      BIGINT       NOT NULL COMMENT '报到批次',
    apply_type    TINYINT      NOT NULL DEFAULT 3 COMMENT '1 绿色通道 / 2 缓交学费 / 3 其他',
    reason        VARCHAR(512) NULL COMMENT '申请理由（加密存储）',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '1 草稿 / 2 待审核 / 3 已通过 / 4 已驳回 / 5 已撤回',
    submit_time   DATETIME(3)  NULL COMMENT '提交时间',
    audit_time    DATETIME(3)  NULL COMMENT '审核时间',
    auditor_id    BIGINT       NULL COMMENT '审核人',
    reject_reason VARCHAR(255) NULL COMMENT '驳回原因',
    version       INT          NOT NULL DEFAULT 0 COMMENT '乐观锁',
    create_time   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by     BIGINT       NULL,
    update_by     BIGINT       NULL,
    deleted       TINYINT(1)   NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_policy_batch (user_id, policy_id, batch_id, deleted),
    KEY idx_status_time (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资助申请';

CREATE TABLE IF NOT EXISTS aid_application_material (
    id              BIGINT PRIMARY KEY COMMENT '雪花 ID',
    application_id  BIGINT       NOT NULL COMMENT '所属申请',
    material_name   VARCHAR(64)  NOT NULL COMMENT '材料名称',
    file_url        VARCHAR(255) NULL COMMENT '文件地址',
    file_hash       VARCHAR(64)  NULL COMMENT '文件指纹',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '1 待补 / 2 已提交 / 3 已通过 / 4 不通过',
    create_time     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by       BIGINT       NULL,
    update_by       BIGINT       NULL,
    deleted         TINYINT(1)   NOT NULL DEFAULT 0,
    KEY idx_application (application_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='申请材料明细';

-- ============================================================
-- life-service
-- ============================================================
CREATE TABLE IF NOT EXISTS life_repair_order (
    id               BIGINT PRIMARY KEY COMMENT '雪花 ID',
    order_no         VARCHAR(32)  NOT NULL COMMENT '单号',
    user_id          BIGINT       NOT NULL COMMENT '报修人',
    dorm_location    VARCHAR(64)  NOT NULL COMMENT '楼栋+房间',
    category         TINYINT      NOT NULL DEFAULT 5 COMMENT '1 水电 / 2 家具 / 3 网络 / 4 门窗 / 5 其他',
    description      VARCHAR(512) NULL COMMENT '故障描述',
    images           JSON         NULL COMMENT '照片地址数组',
    status           TINYINT      NOT NULL DEFAULT 1 COMMENT '1 待受理 / 2 处理中 / 3 已完成 / 4 已取消',
    handler_id       BIGINT       NULL COMMENT '处理人',
    finish_time      DATETIME(3)  NULL COMMENT '完成时间',
    external_order_no VARCHAR(64) NULL COMMENT '后勤系统单号',
    create_time      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by        BIGINT       NULL,
    update_by        BIGINT       NULL,
    deleted          TINYINT(1)   NOT NULL DEFAULT 0,
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_time (user_id, create_time),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报修单';

CREATE TABLE IF NOT EXISTS life_repair_log (
    id          BIGINT PRIMARY KEY COMMENT '雪花 ID',
    order_id    BIGINT       NOT NULL COMMENT '所属报修单',
    from_status TINYINT      NOT NULL COMMENT '变更前状态',
    to_status   TINYINT      NOT NULL COMMENT '变更后状态',
    operator_id BIGINT       NULL COMMENT '操作人',
    note        VARCHAR(255) NULL COMMENT '说明',
    create_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by   BIGINT       NULL,
    update_by   BIGINT       NULL,
    deleted     TINYINT(1)   NOT NULL DEFAULT 0,
    KEY idx_order (order_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报修流转记录';

CREATE TABLE IF NOT EXISTS life_service_info (
    id          BIGINT PRIMARY KEY COMMENT '雪花 ID',
    category    TINYINT       NOT NULL DEFAULT 8 COMMENT '1 洗衣机/热水 / 2 门禁 / 3 食堂 / 4 校园网 / 5 营业厅快递 / 6 就医医保 / 7 心理中心 / 8 FAQ',
    title       VARCHAR(128)  NOT NULL COMMENT '标题',
    content     MEDIUMTEXT    NOT NULL COMMENT '说明内容',
    location    VARCHAR(128)  NULL COMMENT '地点',
    poi_id      BIGINT        NULL COMMENT '关联导航点位',
    phone       VARCHAR(32)   NULL COMMENT '一键拨号号码',
    open_hours  VARCHAR(64)   NULL COMMENT '营业/开放时间',
    extra       JSON          NULL COMMENT '分类特有信息',
    status      TINYINT       NOT NULL DEFAULT 1 COMMENT '1 启用 / 2 停用',
    create_time DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by   BIGINT        NULL,
    update_by   BIGINT        NULL,
    deleted     TINYINT(1)    NOT NULL DEFAULT 0,
    KEY idx_category_status (category, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生活服务信息';

-- ============================================================
-- content-service
-- ============================================================
CREATE TABLE IF NOT EXISTS content_article (
    id            BIGINT PRIMARY KEY COMMENT '雪花 ID',
    module        TINYINT       NOT NULL DEFAULT 1 COMMENT '1 安全教育 / 2 入学教育 / 3 校规校纪 / 4 军训安排 / 5 招新信息',
    title         VARCHAR(128)  NOT NULL COMMENT '标题',
    summary       VARCHAR(255)  NULL COMMENT '摘要',
    content       MEDIUMTEXT    NOT NULL COMMENT '正文',
    cover_url     VARCHAR(255)  NULL COMMENT '封面图',
    source        VARCHAR(128)  NULL COMMENT '来源',
    push_stage    TINYINT       NULL COMMENT '推送阶段',
    start_time    DATETIME(3)   NULL COMMENT '有效期开始',
    end_time      DATETIME(3)   NULL COMMENT '有效期结束',
    poi_id        BIGINT        NULL COMMENT '关联地点',
    status        TINYINT       NOT NULL DEFAULT 1 COMMENT '1 草稿 / 2 已发布 / 3 已下线',
    publish_time  DATETIME(3)   NULL COMMENT '发布时间',
    create_time   DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time   DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by     BIGINT        NULL,
    update_by     BIGINT        NULL,
    deleted       TINYINT(1)    NOT NULL DEFAULT 0,
    KEY idx_module_status (module, status),
    KEY idx_publish_time (publish_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容条目';

CREATE TABLE IF NOT EXISTS content_club (
    id                BIGINT PRIMARY KEY COMMENT '雪花 ID',
    name              VARCHAR(64)  NOT NULL COMMENT '社团名称',
    category          TINYINT      NOT NULL DEFAULT 5 COMMENT '1 学术 / 2 文艺 / 3 体育 / 4 志愿 / 5 其他',
    intro             TEXT         NOT NULL COMMENT '简介',
    contact           VARCHAR(64)  NULL COMMENT '联系方式',
    recruit_article_id BIGINT      NULL COMMENT '关联招新信息',
    status            TINYINT      NOT NULL DEFAULT 1 COMMENT '1 启用 / 2 停用',
    create_time       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by         BIGINT       NULL,
    update_by         BIGINT       NULL,
    deleted           TINYINT(1)   NOT NULL DEFAULT 0,
    KEY idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社团名录';

CREATE TABLE IF NOT EXISTS content_learning_record (
    id          BIGINT PRIMARY KEY COMMENT '雪花 ID',
    user_id     BIGINT       NOT NULL COMMENT '学生',
    article_id  BIGINT       NOT NULL COMMENT '内容条目',
    read_status TINYINT      NOT NULL DEFAULT 1 COMMENT '1 未读 / 2 已读',
    read_time   DATETIME(3)  NULL COMMENT '阅读时间',
    quiz_score  INT          NULL COMMENT '答题得分',
    quiz_detail JSON         NULL COMMENT '答题明细',
    create_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by   BIGINT       NULL,
    update_by   BIGINT       NULL,
    deleted     TINYINT(1)   NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_article (user_id, article_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习记录';

CREATE TABLE IF NOT EXISTS content_file (
    id              BIGINT PRIMARY KEY COMMENT '雪花 ID',
    name            VARCHAR(128) NOT NULL COMMENT '文件名',
    category        TINYINT      NOT NULL DEFAULT 4 COMMENT '1 请假 / 2 证明 / 3 补办卡 / 4 其他',
    file_url        VARCHAR(255) NOT NULL COMMENT '下载地址',
    file_size       INT          NULL COMMENT '字节数',
    file_hash       VARCHAR(64)  NULL COMMENT '校验值',
    download_count  INT          NOT NULL DEFAULT 0 COMMENT '下载次数',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '1 启用 / 2 停用',
    create_time     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by       BIGINT       NULL,
    update_by       BIGINT       NULL,
    deleted         TINYINT(1)   NOT NULL DEFAULT 0,
    KEY idx_category_status (category, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='常用表格文件';

-- ============================================================
-- admin-service
-- ============================================================
CREATE TABLE IF NOT EXISTS admin_user (
    id              BIGINT PRIMARY KEY COMMENT '雪花 ID',
    username        VARCHAR(32)  NOT NULL COMMENT '用户名',
    password_hash   VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码哈希',
    real_name       VARCHAR(32)  NULL COMMENT '真实姓名',
    dept            TINYINT      NOT NULL DEFAULT 5 COMMENT '1 学工 / 2 后勤 / 3 保卫 / 4 学院 / 5 信息中心',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '1 正常 / 2 禁用',
    last_login_time DATETIME(3)  NULL COMMENT '最近登录时间',
    create_time     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by       BIGINT       NULL,
    update_by       BIGINT       NULL,
    deleted         TINYINT(1)   NOT NULL DEFAULT 0,
    UNIQUE KEY uk_username (username),
    KEY idx_dept (dept)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='后台管理员账号';

CREATE TABLE IF NOT EXISTS admin_role (
    id          BIGINT PRIMARY KEY COMMENT '雪花 ID',
    role_code   VARCHAR(32)  NOT NULL COMMENT '角色编码',
    role_name   VARCHAR(64)  NOT NULL COMMENT '角色名称',
    remark      VARCHAR(255) NULL COMMENT '备注',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1 正常 / 2 停用',
    create_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by   BIGINT       NULL,
    update_by   BIGINT       NULL,
    deleted     TINYINT(1)   NOT NULL DEFAULT 0,
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='后台角色';

CREATE TABLE IF NOT EXISTS admin_user_role (
    id            BIGINT PRIMARY KEY COMMENT '雪花 ID',
    admin_user_id BIGINT NOT NULL COMMENT '管理员',
    role_id       BIGINT NOT NULL COMMENT '角色',
    create_time   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by     BIGINT NULL,
    update_by     BIGINT NULL,
    deleted       TINYINT(1) NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_role (admin_user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员角色关联';

CREATE TABLE IF NOT EXISTS admin_role_scope (
    id          BIGINT PRIMARY KEY COMMENT '雪花 ID',
    role_id     BIGINT       NOT NULL COMMENT '角色',
    scope_type  TINYINT      NOT NULL DEFAULT 3 COMMENT '1 学院 / 2 报到批次 / 3 全部',
    scope_value VARCHAR(128) NULL COMMENT '学院码或批次 id',
    create_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by   BIGINT       NULL,
    update_by   BIGINT       NULL,
    deleted     TINYINT(1)   NOT NULL DEFAULT 0,
    KEY idx_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色数据权限';

CREATE TABLE IF NOT EXISTS admin_announcement (
    id            BIGINT PRIMARY KEY COMMENT '雪花 ID',
    title         VARCHAR(128) NOT NULL COMMENT '标题',
    content       MEDIUMTEXT   NOT NULL COMMENT '正文',
    audience      JSON         NULL COMMENT '可见人群',
    publish_time  DATETIME(3)  NULL COMMENT '定时发布',
    revoke_time   DATETIME(3)  NULL COMMENT '定时撤回',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '1 草稿 / 2 已发布 / 3 已撤回',
    publisher_id  BIGINT       NOT NULL COMMENT '发布人',
    create_time   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by     BIGINT       NULL,
    update_by     BIGINT       NULL,
    deleted       TINYINT(1)   NOT NULL DEFAULT 0,
    KEY idx_status_publish (status, publish_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统公告';

CREATE TABLE IF NOT EXISTS admin_config (
    id           BIGINT PRIMARY KEY COMMENT '雪花 ID',
    config_key   VARCHAR(64)  NOT NULL COMMENT '配置键',
    config_value TEXT         NOT NULL COMMENT '配置值',
    config_group VARCHAR(32)  NOT NULL DEFAULT 'common' COMMENT '分组',
    value_type   TINYINT      NOT NULL DEFAULT 1 COMMENT '1 字符串 / 2 数字 / 3 布尔 / 4 JSON',
    remark       VARCHAR(255) NULL COMMENT '说明',
    editable     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否允许后台修改',
    create_time  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by    BIGINT       NULL,
    update_by    BIGINT       NULL,
    deleted      TINYINT(1)   NOT NULL DEFAULT 0,
    UNIQUE KEY uk_config_key (config_key),
    KEY idx_group (config_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置';

CREATE TABLE IF NOT EXISTS admin_operation_log (
    id            BIGINT PRIMARY KEY COMMENT '雪花 ID',
    admin_user_id BIGINT       NOT NULL COMMENT '操作人',
    module        VARCHAR(32)  NOT NULL COMMENT '模块',
    action        VARCHAR(32)  NOT NULL COMMENT '动作',
    target_type   VARCHAR(32)  NULL COMMENT '对象类型',
    target_id     BIGINT       NULL COMMENT '对象 ID',
    detail        TEXT         NULL COMMENT '变更摘要',
    client_ip     VARCHAR(45)  NULL COMMENT '来源 IP',
    result        TINYINT      NOT NULL DEFAULT 1 COMMENT '1 成功 / 2 失败',
    trace_id      VARCHAR(64)  NULL COMMENT '链路 ID',
    create_time   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by     BIGINT       NULL,
    update_by     BIGINT       NULL,
    deleted       TINYINT(1)   NOT NULL DEFAULT 0,
    KEY idx_admin_time (admin_user_id, create_time),
    KEY idx_module_time (module, create_time),
    KEY idx_trace (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='后台操作日志';

CREATE TABLE IF NOT EXISTS admin_behavior_event (
    id          BIGINT PRIMARY KEY COMMENT '雪花 ID',
    event_type  TINYINT      NOT NULL DEFAULT 1 COMMENT '1 问答 / 2 导航 / 3 报到打卡 / 4 内容阅读 / 5 页面访问',
    user_id     BIGINT       NULL COMMENT '用户 ID，匿名可空',
    service     VARCHAR(32)  NULL COMMENT '来源服务',
    ref_id      BIGINT       NULL COMMENT '业务对象 ID',
    channel     TINYINT      NOT NULL DEFAULT 1 COMMENT '1 小程序 / 2 大屏 / 3 后台',
    ext         JSON         NULL COMMENT '附加维度',
    event_time  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '事件发生时间',
    create_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    create_by   BIGINT       NULL,
    update_by   BIGINT       NULL,
    deleted     TINYINT(1)   NOT NULL DEFAULT 0,
    KEY idx_type_time (event_type, event_time),
    KEY idx_user_time (user_id, event_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='埋点事件';

SET FOREIGN_KEY_CHECKS = 1;
