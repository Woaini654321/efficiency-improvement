-- =============================================================================
-- 销售商机互助平台 · 数据库建库脚本（DDL）
-- 数据模型权威源：PRD/商机平台/产品文档-v2.0/01_全局规约手册_v2.md §4 ER + §10 枚举总表
-- 目标库：MySQL 8.0（兼容 5.7）· InnoDB · utf8mb4
-- 库名：sales_lead_hub_server（与 application.yml datasource 一致）
--
-- 设计要点：
--  1) 主键 id：VARCHAR(32)，由 MyBatis-Plus IdType.ASSIGN_ID（雪花）赋值并以字符串存储，
--     与前端「ID 全 string」契约一致；ER 亦定义为 string id PK。
--  2) 枚举一律用 VARCHAR 存储（非 MySQL 原生 ENUM）：便于新增取值免 ALTER（如通知 type 已扩到 13 个），
--     取值由应用层按 §10 枚举总表校验；列注释内联合法值。
--  3) 公共审计列 create_by/update_by/create_time/update_time：由 security-starter 的
--     SecurityMetaObjectHandler 自动填充。⚠️ 字段名须与该 Handler 约定一致，落库前务必核对
--     quectel-code-mysql/security-starter 实际填充的列名（本脚本按 create_*/update_* 常规约定）。
--  4) 乐观锁 version：主并发写实体（user/opportunity/opportunity_request/solution_response/
--     product_line/announcement）加 version INT，更新走 WHERE version=?（MyBatis-Plus @Version）。
--  5) 逻辑删除 is_deleted：opportunity/opportunity_request/interaction 加 is_deleted TINYINT，
--     MyBatis-Plus @TableLogic（0=正常 1=已删）；interaction 软删用于评论占位保留子回复（D7）。
--  6) 冗余快照列（用户硬约束「同一展示字段只来源一张表，绝不跨表拼装」）：
--     publisher_name/publisher_dept_name/responder_name/operator_name/*_names 等直接落在各自实体表，
--     与 FK id 同事务写入；展示读快照列免 JOIN，关系/筛选走 FK 与关联表。
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `sales_lead_hub_server`
  DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_general_ci;
USE `sales_lead_hub_server`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================================================
-- 1. sys_user 用户（表名加 sys_ 前缀避开 MySQL 保留字 user）
--    登录身份来自 UAA（id/username/phone/email/name/tenantId/roles），本地扩展部门/产品线归属等
-- =============================================================================
CREATE TABLE `sys_user` (
  `id`                       VARCHAR(32)   NOT NULL COMMENT '主键(UAA 用户 id，或本地雪花)',
  `username`                 VARCHAR(64)   NOT NULL COMMENT '登录名(UAA)',
  `name`                     VARCHAR(64)   NOT NULL COMMENT '姓名(UAA)',
  `employee_id`             VARCHAR(32)   DEFAULT NULL COMMENT '工号(本地维护，可选)',
  `role`                     VARCHAR(20)   NOT NULL DEFAULT 'sales' COMMENT 'sales/product_manager/admin，单人单角色',
  `department_id`            VARCHAR(32)   DEFAULT NULL COMMENT '部门 FK → sys_department.id',
  `department_name`          VARCHAR(128)  DEFAULT NULL COMMENT '部门名快照',
  `status`                   VARCHAR(20)   NOT NULL DEFAULT 'active' COMMENT 'active/disabled',
  `phone`                    VARCHAR(32)   DEFAULT NULL COMMENT '手机(脱敏见§6)',
  `email`                    VARCHAR(128)  DEFAULT NULL COMMENT '邮箱',
  `avatar`                   VARCHAR(255)  DEFAULT NULL COMMENT '头像 URL',
  `notification_preferences` JSON          DEFAULT NULL COMMENT '通知渠道偏好',
  `expert_tags`              JSON          DEFAULT NULL COMMENT '擅长领域标签',
  `language`                 VARCHAR(10)   NOT NULL DEFAULT 'zh-CN' COMMENT 'zh-CN/en-US',
  `version`                  INT           NOT NULL DEFAULT 0 COMMENT '乐观锁',
  `create_by`                VARCHAR(32)   DEFAULT NULL,
  `update_by`                VARCHAR(32)   DEFAULT NULL,
  `create_time`              DATETIME      DEFAULT NULL,
  `update_time`              DATETIME      DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  UNIQUE KEY `uk_user_employee` (`employee_id`),
  KEY `idx_user_dept` (`department_id`),
  KEY `idx_user_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

-- =============================================================================
-- 2. sys_department 部门（平台本地维护，UAA 不提供；自引用部门树）
-- =============================================================================
CREATE TABLE `sys_department` (
  `id`          VARCHAR(32)  NOT NULL COMMENT '主键',
  `name`        VARCHAR(128) NOT NULL COMMENT '部门名',
  `parent_id`   VARCHAR(32)  DEFAULT NULL COMMENT '父部门 FK(自引用)，NULL=根',
  `owner_id`    VARCHAR(32)  DEFAULT NULL COMMENT '部门负责人 FK → sys_user.id',
  `owner_name`  VARCHAR(64)  DEFAULT NULL COMMENT '负责人姓名快照',
  `sort_order`  INT          NOT NULL DEFAULT 0 COMMENT '同级排序',
  `is_active`   TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `create_time` DATETIME     DEFAULT NULL,
  `update_time` DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_dept_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门';

-- =============================================================================
-- 3. product_line 产品线（运营本地维护；SLA L1 升级人来源）
-- =============================================================================
CREATE TABLE `product_line` (
  `id`          VARCHAR(32)  NOT NULL COMMENT '主键',
  `name`        VARCHAR(128) NOT NULL COMMENT '产品线名',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `is_active`   TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `version`     INT          NOT NULL DEFAULT 0 COMMENT '乐观锁',
  `create_by`   VARCHAR(32)  DEFAULT NULL,
  `update_by`   VARCHAR(32)  DEFAULT NULL,
  `create_time` DATETIME     DEFAULT NULL,
  `update_time` DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pl_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品线';

-- =============================================================================
-- 4. product_line_member 产品线成员（产品线↔用户 + 负责人标记 = L1 升级人）
-- =============================================================================
CREATE TABLE `product_line_member` (
  `id`              VARCHAR(32) NOT NULL COMMENT '主键',
  `product_line_id` VARCHAR(32) NOT NULL COMMENT '产品线 FK',
  `user_id`         VARCHAR(32) NOT NULL COMMENT '用户 FK',
  `user_name`       VARCHAR(64) DEFAULT NULL COMMENT '成员姓名快照',
  `is_owner`        TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '1=产品线负责人(L1升级人)',
  `create_time`     DATETIME    DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plm_line_user` (`product_line_id`, `user_id`),
  KEY `idx_plm_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品线成员';

-- =============================================================================
-- 5. category 分类标签（自引用树；name_en 可选双语，空则英文界面回退中文）
--    与前端 category.json 权威源对齐（CAT01.. / parent_id / sort_order / is_active）
-- =============================================================================
CREATE TABLE `category` (
  `id`          VARCHAR(32)  NOT NULL COMMENT '主键(如 CAT01)',
  `name`        VARCHAR(128) NOT NULL COMMENT '分类名(权威中文名)',
  `name_en`     VARCHAR(128) DEFAULT NULL COMMENT '英文名(决策B，空则回退中文)',
  `parent_id`   VARCHAR(32)  DEFAULT NULL COMMENT '父分类 FK(自引用)，NULL=根',
  `sort_order`  INT          NOT NULL DEFAULT 0 COMMENT '同级排序(仅同级排序 C-5)',
  `is_active`   TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `create_time` DATETIME     DEFAULT NULL,
  `update_time` DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_cat_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类标签';

-- =============================================================================
-- 6. opportunity 商机信息（核心，FSM OPP；全员可见 D4，不加 visibility_scope）
-- =============================================================================
CREATE TABLE `opportunity` (
  `id`                  VARCHAR(32)   NOT NULL COMMENT '主键',
  `title`               VARCHAR(200)  NOT NULL COMMENT '标题',
  `summary`             VARCHAR(500)  DEFAULT NULL COMMENT '摘要',
  `content`             MEDIUMTEXT    DEFAULT NULL COMMENT '富文本正文',
  `type`                VARCHAR(20)   NOT NULL COMMENT 'product_info/solution/success_case',
  `attachments`         JSON          DEFAULT NULL COMMENT '附件[{name,url,size}]',
  `status`              VARCHAR(20)   NOT NULL DEFAULT 'draft' COMMENT 'draft/published/archived',
  `publisher_id`        VARCHAR(32)   NOT NULL COMMENT '发布人 FK',
  `publisher_name`      VARCHAR(64)   DEFAULT NULL COMMENT '发布人姓名快照',
  `department_id`       VARCHAR(32)   DEFAULT NULL COMMENT '发布部门 FK',
  `publisher_dept_name` VARCHAR(128)  DEFAULT NULL COMMENT '发布部门名快照',
  `archived_by`         VARCHAR(32)   DEFAULT NULL COMMENT '下架人 FK(谁下架谁恢复)',
  `category_names`      JSON          DEFAULT NULL COMMENT '分类名快照(展示免JOIN，关系见 opportunity_category)',
  `view_count`          INT           NOT NULL DEFAULT 0 COMMENT '浏览数(去重后自增)',
  `like_count`          INT           NOT NULL DEFAULT 0 COMMENT '点赞数',
  `collect_count`       INT           NOT NULL DEFAULT 0 COMMENT '收藏数',
  `comment_count`       INT           NOT NULL DEFAULT 0 COMMENT '评论数',
  `is_deleted`          TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '逻辑删除 0正常 1删除',
  `version`             INT           NOT NULL DEFAULT 0 COMMENT '乐观锁',
  `create_by`           VARCHAR(32)   DEFAULT NULL,
  `update_by`           VARCHAR(32)   DEFAULT NULL,
  `create_time`         DATETIME      DEFAULT NULL,
  `update_time`         DATETIME      DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_opp_status` (`status`),
  KEY `idx_opp_publisher` (`publisher_id`),
  KEY `idx_opp_type` (`type`),
  KEY `idx_opp_created` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商机信息';

-- =============================================================================
-- 7. opportunity_request 商机需求（核心，FSM REQ；SLA 派生字段）
-- =============================================================================
CREATE TABLE `opportunity_request` (
  `id`                         VARCHAR(32)  NOT NULL COMMENT '主键',
  `title`                      VARCHAR(200) NOT NULL COMMENT '标题',
  `description`                MEDIUMTEXT   DEFAULT NULL COMMENT '需求描述',
  `industry`                   VARCHAR(64)  DEFAULT NULL COMMENT '行业',
  `urgency`                    VARCHAR(20)  NOT NULL DEFAULT 'normal' COMMENT 'normal/urgent/critical(存储)',
  `status`                     VARCHAR(20)  NOT NULL DEFAULT 'Pending' COMMENT 'Pending/Collecting/Adopted/Closed',
  `adopted_response_id`        VARCHAR(32)  DEFAULT NULL COMMENT '采纳方案 FK(采纳单一真相源)',
  `publisher_id`               VARCHAR(32)  NOT NULL COMMENT '发布人 FK',
  `publisher_name`             VARCHAR(64)  DEFAULT NULL COMMENT '发布人姓名快照',
  `department_id`              VARCHAR(32)  DEFAULT NULL COMMENT '发布部门 FK',
  `publisher_dept_name`        VARCHAR(128) DEFAULT NULL COMMENT '发布部门名快照',
  `visibility_scope`           VARCHAR(20)  NOT NULL DEFAULT 'all' COMMENT 'all/dept/personnel(收窄 D4)',
  `visibility_values`          JSON         DEFAULT NULL COMMENT 'dept/personnel 时的具体范围 id 集',
  `invited_product_line_ids`   JSON         DEFAULT NULL COMMENT '邀请产品线 id 集',
  `invited_product_line_names` JSON         DEFAULT NULL COMMENT '邀请产品线名快照(展示免JOIN)',
  `category_names`             JSON         DEFAULT NULL COMMENT '分类名快照(关系见 request_category)',
  `sla_status`                 VARCHAR(20)  NOT NULL DEFAULT 'normal' COMMENT 'normal/warning/overdue/responded(派生)',
  `escalation_level`           VARCHAR(4)   NOT NULL DEFAULT 'L0' COMMENT 'L0/L1/L2/L3',
  `view_count`                 INT          NOT NULL DEFAULT 0 COMMENT '浏览数',
  `response_count`             INT          NOT NULL DEFAULT 0 COMMENT '方案响应数',
  `like_count`                 INT          NOT NULL DEFAULT 0 COMMENT '点赞数',
  `collect_count`              INT          NOT NULL DEFAULT 0 COMMENT '收藏数',
  `comment_count`              INT          NOT NULL DEFAULT 0 COMMENT '评论数',
  `is_deleted`                 TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `version`                    INT          NOT NULL DEFAULT 0 COMMENT '乐观锁',
  `create_by`                  VARCHAR(32)  DEFAULT NULL,
  `update_by`                  VARCHAR(32)  DEFAULT NULL,
  `create_time`                DATETIME     DEFAULT NULL COMMENT 'SLA 首响计时起点',
  `update_time`                DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_req_status` (`status`),
  KEY `idx_req_urgency` (`urgency`),
  KEY `idx_req_sla` (`sla_status`, `escalation_level`),
  KEY `idx_req_publisher` (`publisher_id`),
  KEY `idx_req_created` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商机需求';
-- 注：deadline 已按 D2 移除；超期关闭 = 待响应默认 7 天系统自动关闭，无独立 deadline 列。
-- priority/slaRemaining 由 urgency + create_time 派生，不入库。

-- =============================================================================
-- 8. solution_response 方案响应（需求-方案匹配核心）
-- =============================================================================
CREATE TABLE `solution_response` (
  `id`                     VARCHAR(32)  NOT NULL COMMENT '主键',
  `request_id`             VARCHAR(32)  NOT NULL COMMENT '所属需求 FK',
  `content`                MEDIUMTEXT   DEFAULT NULL COMMENT '富文本方案',
  `attachments`            JSON         DEFAULT NULL COMMENT '附件',
  `responder_id`           VARCHAR(32)  NOT NULL COMMENT '响应人 FK',
  `responder_name`         VARCHAR(64)  DEFAULT NULL COMMENT '响应人姓名快照',
  `department_id`          VARCHAR(32)  DEFAULT NULL COMMENT '响应人部门 FK',
  `responder_dept_name`    VARCHAR(128) DEFAULT NULL COMMENT '响应部门名快照',
  `is_adopted`             TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '派生/冗余，与 request.adopted_response_id 同事务更新',
  `email_recipients`       JSON         DEFAULT NULL COMMENT '邮件通知人',
  `custom_email_recipients` JSON        DEFAULT NULL COMMENT '自定义邮件通知人',
  `feishu_sync`            TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否同步飞书',
  `version`                INT          NOT NULL DEFAULT 0 COMMENT '乐观锁',
  `create_by`              VARCHAR(32)  DEFAULT NULL,
  `update_by`              VARCHAR(32)  DEFAULT NULL,
  `create_time`            DATETIME     DEFAULT NULL,
  `update_time`            DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_resp_request` (`request_id`),
  KEY `idx_resp_responder` (`responder_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='方案响应';
-- 注：score_practicality/completeness/speed 孤儿字段已按 D2 移除(打分下期)。

-- =============================================================================
-- 9. interaction 互动记录（评论≤2级 / 收藏 / 点赞 统一）
-- =============================================================================
CREATE TABLE `interaction` (
  `id`                VARCHAR(32) NOT NULL COMMENT '主键',
  `user_id`           VARCHAR(32) NOT NULL COMMENT '用户 FK',
  `user_name`         VARCHAR(64) DEFAULT NULL COMMENT '用户姓名快照(评论展示)',
  `target_type`       VARCHAR(20) NOT NULL COMMENT 'Opportunity/Request/Response',
  `target_id`         VARCHAR(32) NOT NULL COMMENT '目标 id',
  `type`              VARCHAR(20) NOT NULL COMMENT 'comment/collect/like',
  `content`           TEXT        DEFAULT NULL COMMENT '仅 comment 有值',
  `parent_comment_id` VARCHAR(32) DEFAULT NULL COMMENT '≤2级：NULL=一级评论，非NULL须指向一级评论',
  `is_deleted`        TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '软删占位(D7，保留子回复)',
  `create_time`       DATETIME    DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inter_like_collect` (`user_id`, `target_type`, `target_id`, `type`),
  KEY `idx_inter_target` (`target_type`, `target_id`, `type`),
  KEY `idx_inter_parent` (`parent_comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='互动记录';
-- ⚠️ uk_inter_like_collect 防重复点赞/收藏。comment 不应受此唯一约束限制（同用户可多条评论）：
--    落地方案 = 应用层仅对 type∈{like,collect} 走「存在即删、不存在即插」；comment 走普通 insert。
--    唯一索引对 comment 亦生效，故须保证 comment 不复用此约束路径（或按需拆分两张表，见方案说明）。

-- =============================================================================
-- 10. notification 通知（FSM NOTIF + 强制确认）
-- =============================================================================
CREATE TABLE `notification` (
  `id`               VARCHAR(32)  NOT NULL COMMENT '主键',
  `user_id`          VARCHAR(32)  NOT NULL COMMENT '接收人 FK',
  `batch_id`         VARCHAR(32)  DEFAULT NULL COMMENT '所属推送批次 FK',
  `target_type`      VARCHAR(20)  DEFAULT NULL COMMENT '关联对象类型',
  `target_id`        VARCHAR(32)  DEFAULT NULL COMMENT '关联对象 id',
  `type`             VARCHAR(20)  NOT NULL COMMENT 'publish/response/adopt/system/comment/reply/invite/force_confirm/sla_remind/sla_escalate/archive/category_change/announcement',
  `channel`          VARCHAR(10)  NOT NULL DEFAULT 'in_app' COMMENT 'in_app/feishu/email',
  `title`            VARCHAR(255) DEFAULT NULL COMMENT '通知标题快照',
  `trigger_user_name` VARCHAR(64) DEFAULT NULL COMMENT '触发人姓名快照',
  `is_read`          TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已读',
  `is_force_confirm` TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否强制确认阅读',
  `confirm_time`     DATETIME     DEFAULT NULL COMMENT '强制确认时间',
  `create_time`      DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_notif_user_read` (`user_id`, `is_read`),
  KEY `idx_notif_batch` (`batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知';

-- =============================================================================
-- 11. notification_batch 通知批次（支撑按批次已读率/触达率）
-- =============================================================================
CREATE TABLE `notification_batch` (
  `id`               VARCHAR(32) NOT NULL COMMENT '主键',
  `type`             VARCHAR(20) NOT NULL COMMENT '批次类型(同 notification.type)',
  `source_target_id` VARCHAR(32) DEFAULT NULL COMMENT '触发源对象 id',
  `total_count`      INT         NOT NULL DEFAULT 0 COMMENT '推送总数',
  `read_count`       INT         NOT NULL DEFAULT 0 COMMENT '已读数',
  `confirm_count`    INT         NOT NULL DEFAULT 0 COMMENT '已确认数',
  `pushed_at`        DATETIME    DEFAULT NULL COMMENT '推送时间',
  `create_time`      DATETIME    DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_batch_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知批次';

-- =============================================================================
-- 12. subscription 订阅（独立实体，替代 User.subscriptions JSON，D9）
-- =============================================================================
CREATE TABLE `subscription` (
  `id`            VARCHAR(32) NOT NULL COMMENT '主键',
  `user_id`       VARCHAR(32) NOT NULL COMMENT '订阅用户 FK',
  `category_id`   VARCHAR(32) NOT NULL COMMENT '订阅分类 FK',
  `category_name` VARCHAR(128) DEFAULT NULL COMMENT '分类名快照',
  `source`        VARCHAR(20) NOT NULL DEFAULT 'manual' COMMENT 'default_dept/manual',
  `create_time`   DATETIME    DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sub_user_cat` (`user_id`, `category_id`),
  KEY `idx_sub_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订阅';

-- =============================================================================
-- 13. view_log 浏览记录（24h 去重，唯一键 + 应用层 TTL）
-- =============================================================================
CREATE TABLE `view_log` (
  `id`          VARCHAR(32) NOT NULL COMMENT '主键',
  `user_id`     VARCHAR(32) NOT NULL COMMENT '浏览用户 FK',
  `target_type` VARCHAR(20) NOT NULL COMMENT 'Opportunity/Request',
  `target_id`   VARCHAR(32) NOT NULL COMMENT '目标 id',
  `viewed_at`   DATETIME    NOT NULL COMMENT '浏览时间(24h 去重基准)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_view_user_target` (`user_id`, `target_type`, `target_id`),
  KEY `idx_view_viewed` (`viewed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='浏览记录';
-- 注：TTL 24h 由应用层/定时任务按 viewed_at 判定并刷新（MySQL 无原生 TTL）。

-- =============================================================================
-- 14. audit_log 操作日志（只读追加，不可篡改）
-- =============================================================================
CREATE TABLE `audit_log` (
  `id`              VARCHAR(32)  NOT NULL COMMENT '主键',
  `operator_id`     VARCHAR(32)  DEFAULT NULL COMMENT '操作人 FK',
  `operator_name`   VARCHAR(64)  DEFAULT NULL COMMENT '操作人姓名快照',
  `action_type`     VARCHAR(30)  NOT NULL COMMENT 'publish/archive/delete/role_change/isolation_change/category_change/login/sla_escalation',
  `target`          VARCHAR(255) DEFAULT NULL COMMENT '操作对象描述',
  `result`          VARCHAR(20)  NOT NULL DEFAULT 'success' COMMENT 'success/fail',
  `ip_address`      VARCHAR(64)  DEFAULT NULL COMMENT 'IP(对外脱敏见§6)',
  `user_agent`      VARCHAR(500) DEFAULT NULL COMMENT 'UA',
  `before_snapshot` JSON         DEFAULT NULL COMMENT '变更前快照',
  `after_snapshot`  JSON         DEFAULT NULL COMMENT '变更后快照',
  `create_time`     DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_audit_operator` (`operator_id`),
  KEY `idx_audit_action` (`action_type`),
  KEY `idx_audit_created` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';

-- =============================================================================
-- 15. announcement 公告（运营 PC-24/25；状态 draft/published/archived）
-- =============================================================================
CREATE TABLE `announcement` (
  `id`             VARCHAR(32)  NOT NULL COMMENT '主键',
  `title`          VARCHAR(200) NOT NULL COMMENT '标题',
  `content`        MEDIUMTEXT   DEFAULT NULL COMMENT '富文本正文',
  `type`           VARCHAR(20)  DEFAULT NULL COMMENT 'notice/policy/activity/other',
  `priority`       VARCHAR(10)  NOT NULL DEFAULT 'normal' COMMENT 'high/normal',
  `publisher_id`   VARCHAR(32)  NOT NULL COMMENT '发布人 FK',
  `publisher_name` VARCHAR(64)  DEFAULT NULL COMMENT '发布人姓名快照',
  `is_pinned`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否置顶',
  `status`         VARCHAR(20)  NOT NULL DEFAULT 'draft' COMMENT 'draft/published/archived',
  `view_count`     INT          NOT NULL DEFAULT 0 COMMENT '浏览数',
  `banner_enabled` TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否启用横幅',
  `version`        INT          NOT NULL DEFAULT 0 COMMENT '乐观锁',
  `published_at`   DATETIME     DEFAULT NULL COMMENT '发布时间',
  `create_by`      VARCHAR(32)  DEFAULT NULL,
  `update_by`      VARCHAR(32)  DEFAULT NULL,
  `create_time`    DATETIME     DEFAULT NULL,
  `update_time`    DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ann_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告';

-- =============================================================================
-- 16-17. 关联表（N:M，分类归属；关系真相源，展示走各实体的 category_names 快照）
-- =============================================================================
CREATE TABLE `opportunity_category` (
  `opportunity_id` VARCHAR(32) NOT NULL COMMENT '商机 FK',
  `category_id`    VARCHAR(32) NOT NULL COMMENT '分类 FK',
  PRIMARY KEY (`opportunity_id`, `category_id`),
  KEY `idx_oc_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商机-分类关联';

CREATE TABLE `request_category` (
  `request_id`  VARCHAR(32) NOT NULL COMMENT '需求 FK',
  `category_id` VARCHAR(32) NOT NULL COMMENT '分类 FK',
  PRIMARY KEY (`request_id`, `category_id`),
  KEY `idx_rc_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='需求-分类关联';

-- =============================================================================
-- 18. alert 运营告警（轻量派生表：低发布量/低触达/SLA 超时）
-- =============================================================================
CREATE TABLE `alert` (
  `id`           VARCHAR(32) NOT NULL COMMENT '主键',
  `alert_type`   VARCHAR(20) NOT NULL COMMENT 'low_publish/low_reach/sla_breach',
  `alert_status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/resolved',
  `target_id`    VARCHAR(32) DEFAULT NULL COMMENT '关联对象 id',
  `detail`       JSON        DEFAULT NULL COMMENT '告警详情',
  `resolved_by`  VARCHAR(32) DEFAULT NULL COMMENT '处理人 FK',
  `create_time`  DATETIME    DEFAULT NULL,
  `update_time`  DATETIME    DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_alert_status` (`alert_type`, `alert_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运营告警';

SET FOREIGN_KEY_CHECKS = 1;
-- =============================================================================
-- 说明：
--  · 未建物理外键（FOREIGN KEY 约束）——遵循互联网/MyBatis-Plus 常规，仅建索引，关系约束在应用层保证，
--    利于分库/批量/软删。如需强一致可按需补 FK。
--  · 枚举取值一律以 §10 枚举总表为准，应用层校验；勿在库层用 MySQL ENUM 类型固化。
--  · 初始化种子数据（分类树以前端 category.json 为准：CAT01.. + parent + sort + is_active）建议单独
--    data.sql，勿混入本 schema.sql。
-- =============================================================================
