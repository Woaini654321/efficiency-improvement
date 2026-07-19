-- =============================================================================
-- 销售商机互助平台 · 数据库建库脚本（DDL）
-- 数据模型权威源：PRD/商机平台/产品文档-v2.0/01_全局规约手册_v2.md §4 ER + §10 枚举总表
-- 目标库：MySQL 8.0（兼容 5.7）· InnoDB · utf8mb4 · 库名 sales_lead_hub_server
--
-- 三点定案（已核对 quectel-code starter 字节码 + java-coding 约定）：
--  [决策1·审计列] SecurityMetaObjectHandler 实测自动填充：
--    insert → createTime/updateTime/createBy/updateBy；update → updateTime/updateBy。
--    下划线映射列名 = create_time/update_time/create_by/update_by（本脚本已用此名）。
--    ⚠️ createBy/updateBy 填的是 getCurrentUserId():Long（当前用户 id 为 Long）→ 列类型 BIGINT。
--    实体侧：@TableField(fill=INSERT) 于 createTime/createBy；fill=INSERT_UPDATE 于 updateTime/updateBy。
--  [决策·主键] 用户 id 由框架定为 Long ⇒ 全表主键统一 BIGINT + MyBatis-Plus IdType.ASSIGN_ID(雪花)；
--    前端「ID 全 string」由全局 Jackson Long→String(ToStringSerializer/JsonFormat) 满足，DB 存 BIGINT。
--  [决策2·互动单表] 遵循 ER 单一 Interaction 表；PRD §4.3「like/collect 唯一、comment 不受约束」用
--    MySQL 生成列实现：reaction_uk 对 comment 为 NULL(唯一索引放行多 NULL)，对 like/collect 唯一。
--  [决策3·执行] schema.sql 为唯一建表真相源，受版本管理；不在生产自动跑 DDL（§7 生产账号无建库/建表权）：
--    · 本地 dev：手动执行 schema.sql + data.sql（createDatabaseIfNotExist 仅自建空库，不建表）。
--    · 生产：DBA 以最小权限账号执行；不启用 spring.sql.init 自动 DDL。未引入 Flyway/Liquibase(避免新依赖)。
--
-- 其它约定：
--  · 枚举一律 VARCHAR + 应用层按 §10 校验（非 MySQL 原生 ENUM，便于加值免 ALTER）；列注释内联合法值。
--  · 乐观锁 version(@Version)：user/opportunity/opportunity_request/solution_response/product_line/announcement。
--    乐观锁拦截器由 mysql-starter 的 MyBatisPlusConfig 提供（文档权威）；业务禁自定义 MyBatisPlusConfig(Bean 冲突)。
--  · 逻辑删除(mysql-starter 框架约定)：字段名 `deleted` CHAR(1)，'Y'删/'N'正常，MyBatis-Plus 全局管理，
--    removeById 自动置'Y'、查询自动过滤，业务禁手动赋值。用于 opportunity/opportunity_request。
--  · interaction 不用框架 deleted：like/collect 取消走物理删(removeById，配合 reaction_uk 唯一键复用)；
--    评论 D7 软删用独立业务字段 content_deleted(行保留、内容转占位、子回复保留)，与框架"隐藏行"语义不同。
--  · 冗余快照列（用户硬约束「一展示字段一表、绝不跨表拼装」）：publisher_name/responder_name/*_names 等
--    与 FK id 同事务写入，展示读快照免 JOIN；关系/筛选走 FK 与关联表。
--  · 不建物理外键（MyBatis-Plus 常规，关系在应用层保证，利于软删/批量/分库）；仅建索引。
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `sales_lead_hub_server`
  DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_general_ci;
USE `sales_lead_hub_server`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================================================
-- 1. sys_user 用户（表名 sys_ 前缀避开保留字 user；id = UAA 的 Long 用户 id）
-- =============================================================================
CREATE TABLE `sys_user` (
  `id`                       BIGINT        NOT NULL COMMENT '主键(UAA 用户 id，Long)',
  `username`                 VARCHAR(64)   NOT NULL COMMENT '登录名(UAA)',
  `name`                     VARCHAR(64)   NOT NULL COMMENT '姓名(UAA)',
  `employee_id`             VARCHAR(32)   DEFAULT NULL COMMENT '工号(本地维护，可选)',
  `role`                     VARCHAR(20)   NOT NULL DEFAULT 'sales' COMMENT 'sales/product_manager/admin，单人单角色',
  `department_id`            BIGINT        DEFAULT NULL COMMENT '部门 FK → sys_department.id',
  `department_name`          VARCHAR(128)  DEFAULT NULL COMMENT '部门名快照',
  `status`                   VARCHAR(20)   NOT NULL DEFAULT 'active' COMMENT 'active/disabled',
  `phone`                    VARCHAR(32)   DEFAULT NULL COMMENT '手机(脱敏见§6)',
  `email`                    VARCHAR(128)  DEFAULT NULL COMMENT '邮箱',
  `avatar`                   VARCHAR(255)  DEFAULT NULL COMMENT '头像 URL',
  `notification_preferences` JSON          DEFAULT NULL COMMENT '通知渠道偏好',
  `expert_tags`              JSON          DEFAULT NULL COMMENT '擅长领域标签',
  `language`                 VARCHAR(10)   NOT NULL DEFAULT 'zh-CN' COMMENT 'zh-CN/en-US',
  `version`                  INT           NOT NULL DEFAULT 0 COMMENT '乐观锁',
  `create_by`                BIGINT        DEFAULT NULL,
  `update_by`                BIGINT        DEFAULT NULL,
  `create_time`              DATETIME      DEFAULT NULL,
  `update_time`              DATETIME      DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  UNIQUE KEY `uk_user_employee` (`employee_id`),
  KEY `idx_user_dept` (`department_id`),
  KEY `idx_user_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

-- =============================================================================
-- 2. sys_department 部门（平台本地维护；自引用部门树）
-- =============================================================================
CREATE TABLE `sys_department` (
  `id`          BIGINT       NOT NULL COMMENT '主键',
  `name`        VARCHAR(128) NOT NULL COMMENT '部门名',
  `parent_id`   BIGINT       DEFAULT NULL COMMENT '父部门 FK(自引用)，NULL=根',
  `owner_id`    BIGINT       DEFAULT NULL COMMENT '部门负责人 FK → sys_user.id',
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
  `id`          BIGINT       NOT NULL COMMENT '主键',
  `name`        VARCHAR(128) NOT NULL COMMENT '产品线名',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `is_active`   TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `version`     INT          NOT NULL DEFAULT 0 COMMENT '乐观锁',
  `create_by`   BIGINT       DEFAULT NULL,
  `update_by`   BIGINT       DEFAULT NULL,
  `create_time` DATETIME     DEFAULT NULL,
  `update_time` DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pl_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品线';

-- =============================================================================
-- 4. product_line_member 产品线成员（产品线↔用户 + 负责人标记 = L1 升级人）
-- =============================================================================
CREATE TABLE `product_line_member` (
  `id`              BIGINT      NOT NULL COMMENT '主键',
  `product_line_id` BIGINT      NOT NULL COMMENT '产品线 FK',
  `user_id`         BIGINT      NOT NULL COMMENT '用户 FK',
  `user_name`       VARCHAR(64) DEFAULT NULL COMMENT '成员姓名快照',
  `is_owner`        TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '1=产品线负责人(L1升级人)',
  `create_time`     DATETIME    DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plm_line_user` (`product_line_id`, `user_id`),
  KEY `idx_plm_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品线成员';

-- =============================================================================
-- 5. category 分类标签（自引用树；对齐前端 category.json 权威源：parent/sort/is_active）
--    id=雪花 BIGINT；前端 CAT01 等为 mock 展示码，非 DB 主键
-- =============================================================================
CREATE TABLE `category` (
  `id`          BIGINT       NOT NULL COMMENT '主键(雪花)',
  `name`        VARCHAR(128) NOT NULL COMMENT '分类名(权威中文名)',
  `name_en`     VARCHAR(128) DEFAULT NULL COMMENT '英文名(决策B，空则回退中文)',
  `parent_id`   BIGINT       DEFAULT NULL COMMENT '父分类 FK(自引用)，NULL=根',
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
  `id`                  BIGINT        NOT NULL COMMENT '主键(雪花)',
  `title`               VARCHAR(200)  NOT NULL COMMENT '标题',
  `summary`             VARCHAR(500)  DEFAULT NULL COMMENT '摘要',
  `content`             MEDIUMTEXT    DEFAULT NULL COMMENT '富文本正文',
  `type`                VARCHAR(20)   NOT NULL COMMENT 'product_info/solution/success_case',
  `attachments`         JSON          DEFAULT NULL COMMENT '附件[{name,url,size}]',
  `status`              VARCHAR(20)   NOT NULL DEFAULT 'draft' COMMENT 'draft/published/archived',
  `publisher_id`        BIGINT        NOT NULL COMMENT '发布人 FK',
  `publisher_name`      VARCHAR(64)   DEFAULT NULL COMMENT '发布人姓名快照',
  `department_id`       BIGINT        DEFAULT NULL COMMENT '发布部门 FK',
  `publisher_dept_name` VARCHAR(128)  DEFAULT NULL COMMENT '发布部门名快照',
  `archived_by`         BIGINT        DEFAULT NULL COMMENT '下架人 FK(谁下架谁恢复)',
  `category_names`      JSON          DEFAULT NULL COMMENT '分类名快照(展示免JOIN，关系见 opportunity_category)',
  `view_count`          INT           NOT NULL DEFAULT 0 COMMENT '浏览数(去重后自增)',
  `like_count`          INT           NOT NULL DEFAULT 0 COMMENT '点赞数',
  `collect_count`       INT           NOT NULL DEFAULT 0 COMMENT '收藏数',
  `comment_count`       INT           NOT NULL DEFAULT 0 COMMENT '评论数',
  `deleted`             CHAR(1)       NOT NULL DEFAULT 'N' COMMENT '逻辑删除 Y删/N正常(框架管理,勿手动置)',
  `version`             INT           NOT NULL DEFAULT 0 COMMENT '乐观锁',
  `create_by`           BIGINT        DEFAULT NULL,
  `update_by`           BIGINT        DEFAULT NULL,
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
  `id`                         BIGINT       NOT NULL COMMENT '主键(雪花)',
  `title`                      VARCHAR(200) NOT NULL COMMENT '标题',
  `description`                MEDIUMTEXT   DEFAULT NULL COMMENT '需求描述',
  `industry`                   VARCHAR(64)  DEFAULT NULL COMMENT '行业',
  `urgency`                    VARCHAR(20)  NOT NULL DEFAULT 'normal' COMMENT 'normal/urgent/critical(存储)',
  `status`                     VARCHAR(20)  NOT NULL DEFAULT 'Pending' COMMENT 'Pending/Collecting/Adopted/Closed',
  `adopted_response_id`        BIGINT       DEFAULT NULL COMMENT '采纳方案 FK(采纳单一真相源)',
  `publisher_id`               BIGINT       NOT NULL COMMENT '发布人 FK',
  `publisher_name`             VARCHAR(64)  DEFAULT NULL COMMENT '发布人姓名快照',
  `department_id`              BIGINT       DEFAULT NULL COMMENT '发布部门 FK',
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
  `deleted`                    CHAR(1)      NOT NULL DEFAULT 'N' COMMENT '逻辑删除 Y删/N正常(框架管理,勿手动置)',
  `version`                    INT          NOT NULL DEFAULT 0 COMMENT '乐观锁',
  `create_by`                  BIGINT       DEFAULT NULL,
  `update_by`                  BIGINT       DEFAULT NULL,
  `create_time`                DATETIME     DEFAULT NULL COMMENT 'SLA 首响计时起点',
  `update_time`                DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_req_status` (`status`),
  KEY `idx_req_urgency` (`urgency`),
  KEY `idx_req_sla` (`sla_status`, `escalation_level`),
  KEY `idx_req_publisher` (`publisher_id`),
  KEY `idx_req_created` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商机需求';
-- 注：deadline 已按 D2 移除；超期关闭 = 待响应默认 7 天系统自动关闭。
-- priority/slaRemaining 由 urgency + create_time 派生，不入库。

-- =============================================================================
-- 8. solution_response 方案响应（需求-方案匹配核心）
-- =============================================================================
CREATE TABLE `solution_response` (
  `id`                     BIGINT       NOT NULL COMMENT '主键(雪花)',
  `request_id`             BIGINT       NOT NULL COMMENT '所属需求 FK',
  `content`                MEDIUMTEXT   DEFAULT NULL COMMENT '富文本方案',
  `attachments`            JSON         DEFAULT NULL COMMENT '附件',
  `responder_id`           BIGINT       NOT NULL COMMENT '响应人 FK',
  `responder_name`         VARCHAR(64)  DEFAULT NULL COMMENT '响应人姓名快照',
  `department_id`          BIGINT       DEFAULT NULL COMMENT '响应人部门 FK',
  `responder_dept_name`    VARCHAR(128) DEFAULT NULL COMMENT '响应部门名快照',
  `is_adopted`             TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '派生/冗余，与 request.adopted_response_id 同事务更新',
  `email_recipients`       JSON         DEFAULT NULL COMMENT '邮件通知人',
  `custom_email_recipients` JSON        DEFAULT NULL COMMENT '自定义邮件通知人',
  `feishu_sync`            TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否同步飞书',
  `version`                INT          NOT NULL DEFAULT 0 COMMENT '乐观锁',
  `create_by`              BIGINT       DEFAULT NULL,
  `update_by`              BIGINT       DEFAULT NULL,
  `create_time`            DATETIME     DEFAULT NULL,
  `update_time`            DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_resp_request` (`request_id`),
  KEY `idx_resp_responder` (`responder_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='方案响应';
-- 注：score_practicality/completeness/speed 孤儿字段已按 D2 移除(打分下期)。

-- =============================================================================
-- 9. interaction 互动记录（评论≤2级 / 收藏 / 点赞 统一 —— ER 单表）
--    [决策2] like/collect 唯一、comment 不受约束：用生成列 reaction_uk 实现
--    （comment 行 reaction_uk=NULL，唯一索引放行多 NULL；like/collect 行有值→唯一）
-- =============================================================================
CREATE TABLE `interaction` (
  `id`                BIGINT      NOT NULL COMMENT '主键(雪花)',
  `user_id`           BIGINT      NOT NULL COMMENT '用户 FK',
  `user_name`         VARCHAR(64) DEFAULT NULL COMMENT '用户姓名快照(评论展示)',
  `target_type`       VARCHAR(20) NOT NULL COMMENT 'Opportunity/Request/Response',
  `target_id`         BIGINT      NOT NULL COMMENT '目标 id',
  `type`              VARCHAR(20) NOT NULL COMMENT 'comment/collect/like',
  `content`           TEXT        DEFAULT NULL COMMENT '仅 comment 有值',
  `parent_comment_id` BIGINT      DEFAULT NULL COMMENT '≤2级：NULL=一级评论，非NULL须指向一级评论',
  `content_deleted`   TINYINT(1)  NOT NULL DEFAULT 0 COMMENT 'D7评论软删占位(行保留/内容转占位/留子回复)，非框架 deleted',
  `create_time`       DATETIME    DEFAULT NULL,
  `reaction_uk`       VARCHAR(96) GENERATED ALWAYS AS (
                        CASE WHEN `type` IN ('like','collect')
                             THEN CONCAT_WS(':', `user_id`, `target_type`, `target_id`, `type`)
                             ELSE NULL END) STORED
                        COMMENT 'like/collect 去重键；comment 为 NULL 不受唯一约束',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inter_reaction` (`reaction_uk`),
  KEY `idx_inter_target` (`target_type`, `target_id`, `type`),
  KEY `idx_inter_parent` (`parent_comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='互动记录';

-- =============================================================================
-- 10. notification 通知（FSM NOTIF + 强制确认）
-- =============================================================================
CREATE TABLE `notification` (
  `id`               BIGINT       NOT NULL COMMENT '主键(雪花)',
  `user_id`          BIGINT       NOT NULL COMMENT '接收人 FK',
  `batch_id`         BIGINT       DEFAULT NULL COMMENT '所属推送批次 FK',
  `target_type`      VARCHAR(20)  DEFAULT NULL COMMENT '关联对象类型',
  `target_id`        BIGINT       DEFAULT NULL COMMENT '关联对象 id',
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
  `id`               BIGINT      NOT NULL COMMENT '主键(雪花)',
  `type`             VARCHAR(20) NOT NULL COMMENT '批次类型(同 notification.type)',
  `source_target_id` BIGINT      DEFAULT NULL COMMENT '触发源对象 id',
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
  `id`            BIGINT      NOT NULL COMMENT '主键(雪花)',
  `user_id`       BIGINT      NOT NULL COMMENT '订阅用户 FK',
  `category_id`   BIGINT      NOT NULL COMMENT '订阅分类 FK',
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
  `id`          BIGINT      NOT NULL COMMENT '主键(雪花)',
  `user_id`     BIGINT      NOT NULL COMMENT '浏览用户 FK',
  `target_type` VARCHAR(20) NOT NULL COMMENT 'Opportunity/Request',
  `target_id`   BIGINT      NOT NULL COMMENT '目标 id',
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
  `id`              BIGINT       NOT NULL COMMENT '主键(雪花)',
  `operator_id`     BIGINT       DEFAULT NULL COMMENT '操作人 FK',
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
  `id`             BIGINT       NOT NULL COMMENT '主键(雪花)',
  `title`          VARCHAR(200) NOT NULL COMMENT '标题',
  `content`        MEDIUMTEXT   DEFAULT NULL COMMENT '富文本正文',
  `type`           VARCHAR(20)  DEFAULT NULL COMMENT 'notice/policy/activity/other',
  `priority`       VARCHAR(10)  NOT NULL DEFAULT 'normal' COMMENT 'high/normal',
  `publisher_id`   BIGINT       NOT NULL COMMENT '发布人 FK',
  `publisher_name` VARCHAR(64)  DEFAULT NULL COMMENT '发布人姓名快照',
  `is_pinned`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否置顶',
  `status`         VARCHAR(20)  NOT NULL DEFAULT 'draft' COMMENT 'draft/published/archived',
  `view_count`     INT          NOT NULL DEFAULT 0 COMMENT '浏览数',
  `banner_enabled` TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否启用横幅',
  `version`        INT          NOT NULL DEFAULT 0 COMMENT '乐观锁',
  `published_at`   DATETIME     DEFAULT NULL COMMENT '发布时间',
  `create_by`      BIGINT       DEFAULT NULL,
  `update_by`      BIGINT       DEFAULT NULL,
  `create_time`    DATETIME     DEFAULT NULL,
  `update_time`    DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ann_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告';

-- =============================================================================
-- 16-17. 关联表（N:M，分类归属；关系真相源，展示走各实体 category_names 快照）
-- =============================================================================
CREATE TABLE `opportunity_category` (
  `opportunity_id` BIGINT NOT NULL COMMENT '商机 FK',
  `category_id`    BIGINT NOT NULL COMMENT '分类 FK',
  PRIMARY KEY (`opportunity_id`, `category_id`),
  KEY `idx_oc_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商机-分类关联';

CREATE TABLE `request_category` (
  `request_id`  BIGINT NOT NULL COMMENT '需求 FK',
  `category_id` BIGINT NOT NULL COMMENT '分类 FK',
  PRIMARY KEY (`request_id`, `category_id`),
  KEY `idx_rc_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='需求-分类关联';

-- =============================================================================
-- 18. alert 运营告警（轻量派生表：低发布量/低触达/SLA 超时）
-- =============================================================================
CREATE TABLE `alert` (
  `id`           BIGINT      NOT NULL COMMENT '主键(雪花)',
  `alert_type`   VARCHAR(20) NOT NULL COMMENT 'low_publish/low_reach/sla_breach',
  `alert_status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/resolved',
  `target_id`    BIGINT      DEFAULT NULL COMMENT '关联对象 id',
  `detail`       JSON        DEFAULT NULL COMMENT '告警详情',
  `resolved_by`  BIGINT      DEFAULT NULL COMMENT '处理人 FK',
  `create_time`  DATETIME    DEFAULT NULL,
  `update_time`  DATETIME    DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_alert_status` (`alert_type`, `alert_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运营告警';

SET FOREIGN_KEY_CHECKS = 1;
-- =============================================================================
-- 种子数据：分类树（以前端 category.json 为准）+ 角色用户/产品线，请单独出 data.sql，勿混入本文件。
-- =============================================================================
