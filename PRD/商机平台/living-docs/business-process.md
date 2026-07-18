# Quectel商机信息发布平台 · 业务流程编译产出

> **数据源**：`research.md` §三角色画像 + §五现状流转叙事 + §七核心实体速写
> **渲染时间**：2026-05-27
> **角色注册表**：ROLE-01 销售人员 | ROLE-02 产品经理 | ROLE-03 运营管理员
> **节点**：/2 Process
> **版本**：v1.0

---

## 模块 A：跨职能泳道图（To-Be 系统流程）

### A-1 产品信息发布与获取

```mermaid
flowchart TB
    subgraph ROLE_02["ROLE-02 产品经理"]
        A1_start(["开始：产品经理准备新方案"])
        A1_create["创建: 商机信息（富文本+附件）"]
        A1_category["选择: 分类标签（多级）"]
        A1_save{"是否保存为草稿？"}
        A1_draft["保存: 草稿状态"]
        A1_publish["发布: 商机信息（状态→已发布）"]
    end

    subgraph SYSTEM["系统自动处理"]
        A1_notify["推送: 多渠道通知（站内+飞书+邮件）"]
        A1_index["索引: 更新搜索索引"]
    end

    subgraph ROLE_01["ROLE-01 销售人员"]
        A1_receive["接收: 通知提醒"]
        A1_browse["浏览: 找方案（分类筛选）"]
        A1_search["搜索: 关键词检索方案"]
        A1_detail["查看: 商机详情页"]
        A1_interact{"是否互动？"}
        A1_action["操作: 评论/收藏/点赞"]
        A1_end(["结束：销售获取方案信息"])
    end

    A1_start --> A1_create --> A1_category --> A1_save
    A1_save -->|是| A1_draft --> A1_publish
    A1_save -->|否| A1_publish
    A1_publish --> A1_notify
    A1_publish --> A1_index
    A1_notify --> A1_receive --> A1_detail
    A1_index --> A1_browse
    A1_browse --> A1_search --> A1_detail
    A1_detail --> A1_interact
    A1_interact -->|是| A1_action --> A1_end
    A1_interact -->|否| A1_end
```

### A-2 通知订阅与触达

```mermaid
flowchart TB
    subgraph ROLE_01["ROLE-01 销售人员"]
        A2_start(["开始：销售配置订阅"])
        A2_subscribe["配置: 订阅分类/产品线"]
        A2_channel["选择: 通知渠道偏好（站内/飞书/邮件）"]
        A2_receive["接收: 匹配通知"]
        A2_read{"是否查看？"}
        A2_view["查看: 详情内容"]
        A2_ignore["忽略: 标记已读"]
        A2_end(["结束：通知触达完成"])
    end

    subgraph SYSTEM["系统自动处理"]
        A2_match["匹配: 订阅规则过滤"]
        A2_push["推送: 按渠道分发通知"]
        A2_track["记录: 已读/未读状态"]
    end

    subgraph ROLE_02["ROLE-02 产品经理"]
        A2_pub["发布: 新内容上线"]
    end

    A2_start --> A2_subscribe --> A2_channel
    A2_pub --> A2_match --> A2_push --> A2_receive
    A2_receive --> A2_read
    A2_read -->|是| A2_view --> A2_track --> A2_end
    A2_read -->|否| A2_ignore --> A2_track
    A2_track --> A2_end
```

### A-3 商机需求-方案匹配与采纳

```mermaid
flowchart TB
    subgraph ROLE_01["ROLE-01 销售人员"]
        A3_start(["开始：销售发现客户需求"])
        A3_create["发布: 商机需求（标题/描述/行业/紧急程度）"]
        A3_wait["等待: 方案响应"]
        A3_review["评估: 收到的方案"]
        A3_adopt{"是否采纳？"}
        A3_mark["标记: 最佳方案（状态→已采纳）"]
        A3_close["关闭: 需求（状态→已关闭）"]
        A3_end(["结束：需求-方案匹配完成"])
    end

    subgraph SYSTEM["系统自动处理"]
        A3_notify_pm["通知: 匹配产品线（按分类/专家标签）"]
        A3_notify_sales["通知: 有新方案响应"]
        A3_sla["监控: SLA响应时效"]
    end

    subgraph ROLE_02["ROLE-02 产品经理"]
        A3_receive["接收: 需求通知"]
        A3_respond["响应: 提供方案（富文本+附件）"]
    end

    A3_start --> A3_create --> A3_notify_pm
    A3_notify_pm --> A3_receive --> A3_respond
    A3_respond --> A3_notify_sales --> A3_wait
    A3_create --> A3_sla
    A3_wait --> A3_review --> A3_adopt
    A3_adopt -->|是| A3_mark --> A3_close --> A3_end
    A3_adopt -->|否| A3_wait
```

---

## 模块 B：有限状态机（FSM）

### B-1 商机信息（Opportunity）状态机

```mermaid
stateDiagram-v2
    [*] --> 草稿 : 产品经理创建内容
    草稿 --> 已发布 : 点击发布
    已发布 --> 已下架 : 产品经理/管理员下架
    已下架 --> 已发布 : 重新上架
    已下架 --> [*] : 彻底删除

    note right of 草稿
        进入：创建新内容
        退出：发布或删除
    end note

    note right of 已发布
        进入：发布操作
        退出：下架
        约束：触发多渠道通知
    end note

    note right of 已下架
        进入：下架操作
        退出：重新上架或删除
    end note
```

**状态字典表**

| 状态 | 英文 | 进入条件 | 退出条件 | 关键约束 |
|------|------|----------|----------|----------|
| 草稿 | Draft | 产品经理创建新商机信息 | 发布 / 删除 | 仅创建者可编辑 |
| 已发布 | Published | 点击发布按钮 | 下架 | 触发通知推送；内容可被搜索浏览 |
| 已下架 | Archived | 产品经理或管理员执行下架 | 重新上架 / 彻底删除 | 前台不可见；可恢复 |

### B-2 商机需求（OpportunityRequest）状态机

```mermaid
stateDiagram-v2
    [*] --> 待响应 : 销售发布需求
    待响应 --> 方案收集中 : 收到首个方案响应
    方案收集中 --> 已采纳 : 发布者标记最佳方案
    方案收集中 --> 已关闭 : 发布者手动关闭/超期自动关闭
    已采纳 --> 已关闭 : 需求完结归档
    已关闭 --> [*]

    note right of 待响应
        进入：销售提交需求
        退出：收到方案 / 超期
        约束：触发SLA计时
    end note

    note right of 方案收集中
        进入：首个方案到达
        退出：采纳或关闭
    end note

    note right of 已采纳
        进入：标记最佳方案
        退出：归档关闭
        约束：adopted_response_id 写入
    end note

    note right of 已关闭
        进入：手动关闭/超期/采纳后归档
        退出：终态
    end note
```

**状态字典表**

| 状态 | 英文 | 进入条件 | 退出条件 | 关键约束 |
|------|------|----------|----------|----------|
| 待响应 | Pending | 销售发布需求 | 收到首个方案 / 超期关闭 | 启动SLA计时器 |
| 方案收集中 | Collecting | 首个方案响应到达 | 采纳 / 手动关闭 / 超期关闭 | 可持续接收方案 |
| 已采纳 | Adopted | 发布者标记最佳方案 | 归档关闭 | 写入 adopted_response_id |
| 已关闭 | Closed | 手动关闭 / 超期 / 采纳后归档 | 终态 | 不可重开 |

### B-3 通知（Notification）状态机

```mermaid
stateDiagram-v2
    [*] --> 未读 : 系统生成通知
    未读 --> 已读 : 用户查看
    已读 --> [*]

    note right of 未读
        进入：系统推送
        退出：用户点击查看
    end note

    note right of 已读
        进入：用户查看
        退出：终态
    end note
```

**状态字典表**

| 状态 | 英文 | 进入条件 | 退出条件 | 关键约束 |
|------|------|----------|----------|----------|
| 未读 | Unread | 系统生成通知推送 | 用户点击查看 | 计入未读角标 |
| 已读 | Read | 用户查看通知 | 终态 | 从未读列表移除 |

---

## 模块 C：核心实体关系图（ER）

```mermaid
erDiagram
    User {
        string id PK
        string name
        string employee_id UK
        enum role "销售/产品经理/管理员"
        string department_id FK
        json subscriptions "订阅分类列表"
        json expert_tags "擅长领域"
        json notification_preferences "站内/飞书/邮件"
        enum language "中/英"
    }

    Opportunity {
        string id PK
        string title
        string summary
        text content "富文本"
        enum type "产品信息/解决方案/成功案例"
        json attachments
        enum status "草稿/已发布/已下架"
        enum source "手动发布/接口同步"
        string publisher_id FK "→User"
        string department_id FK
        int view_count
        int like_count
        int collect_count
        int comment_count
        datetime created_at
        datetime updated_at
    }

    OpportunityRequest {
        string id PK
        string title
        text description "富文本"
        string industry "行业/场景"
        enum urgency "普通/紧急/特急"
        enum status "待响应/方案收集中/已采纳/已关闭"
        string adopted_response_id FK "→SolutionResponse"
        string publisher_id FK "→User"
        string department_id FK
        int view_count
        int response_count
        datetime created_at
        datetime updated_at
    }

    SolutionResponse {
        string id PK
        string request_id FK "→OpportunityRequest"
        text content "富文本"
        json attachments
        string responder_id FK "→User"
        string department_id FK
        boolean is_adopted
        float score_practicality "预留"
        float score_completeness "预留"
        float score_speed "预留"
        datetime created_at
        datetime updated_at
    }

    Category {
        string id PK
        string name
        string parent_id FK "→Category（自引用）"
        int sort_order
        boolean is_active
    }

    Interaction {
        string id PK
        string user_id FK "→User"
        enum target_type "Opportunity/Request"
        string target_id
        enum type "评论/收藏/点赞"
        text content "评论内容"
        datetime created_at
    }

    Notification {
        string id PK
        string user_id FK "→User"
        enum target_type
        string target_id
        enum type "发布/响应/采纳/系统"
        enum channel "站内/飞书/邮件"
        boolean is_read
        datetime created_at
    }

    User ||--o{ Opportunity : "发布"
    User ||--o{ OpportunityRequest : "发布"
    User ||--o{ SolutionResponse : "响应"
    User ||--o{ Interaction : "产生"
    User ||--o{ Notification : "接收"

    Opportunity }o--o{ Category : "归属（N:N via opportunity_category）"
    OpportunityRequest }o--o{ Category : "归属（N:N via request_category）"

    OpportunityRequest ||--o{ SolutionResponse : "收到"
    OpportunityRequest ||--o| SolutionResponse : "采纳"

    Opportunity ||--o{ Interaction : "被互动"
    OpportunityRequest ||--o{ Interaction : "被互动"

    Opportunity ||--o{ Notification : "触发"
    OpportunityRequest ||--o{ Notification : "触发"
    SolutionResponse ||--o{ Notification : "触发"

    Category ||--o{ Category : "父子层级"
```

---

## 模块 D：SLA 规则矩阵

| 优先级 | 级别名称 | 首次响应时限 | 解决时限 | 触发场景示例 | 超时升级对象 |
|--------|----------|-------------|----------|-------------|-------------|
| P0 | 特急 | 2h | 24h | 紧急程度=特急的商机需求；重大客户项目机会 | L1→产品线负责人 → L2→STKH-02产品管理部负责人 → L3→STKH-04管理层 |
| P1 | 紧急 | 4h | 48h | 紧急程度=紧急的商机需求；限时投标项目 | L1→产品线负责人 → L2→STKH-02产品管理部负责人 |
| P2 | 普通 | 24h | 5个工作日 | 紧急程度=普通的常规需求 | L1→产品线负责人 |
| — | 内容发布通知 | 30min（触达） | — | 商机信息发布后通知推送 | 系统告警→ROLE-03运营管理员 |
| — | 搜索性能 | — | — | 页面加载≤2s；搜索响应≤1s | 技术告警→STKH-03 IT部门 |

> 数据来源：§六.4 NFR约束卡片（并发性能+可用性SLA）+ RISK-02（产品线不响应）+ ITIL最佳实践

---

## 模块 E：异常分支与红线规则

### E-1 SLA超时升级链（溯源：RISK-02 + SLA矩阵）

```mermaid
flowchart TB
    E1_start(["触发：需求响应超时"])
    E1_l1["L1升级: 通知产品线负责人"]
    E1_check1{"L1响应？"}
    E1_l2["L2升级: 通知STKH-02产品管理部负责人"]
    E1_check2{"L2响应？"}
    E1_l3["L3升级: 通知STKH-04管理层"]
    E1_resolve["处理: 指定责任人强制响应"]
    E1_end(["结束：升级链闭环"])

    E1_start --> E1_l1 --> E1_check1
    E1_check1 -->|是| E1_end
    E1_check1 -->|否| E1_l2 --> E1_check2
    E1_check2 -->|是| E1_end
    E1_check2 -->|否| E1_l3 --> E1_resolve --> E1_end
```

### E-2 新方案发布无人知晓（溯源：翻车记录1 + PAIN-002）

```mermaid
flowchart TB
    E2_start(["触发：方案发布后48h"])
    E2_check["检测: 通知已读率"]
    E2_gate{"已读率≥80%？"}
    E2_ok(["正常：触达达标"])
    E2_alert["告警: 推送ROLE-03运营管理员"]
    E2_action["处理: 二次推送+渠道切换"]
    E2_end(["结束：确保触达"])

    E2_start --> E2_check --> E2_gate
    E2_gate -->|是| E2_ok
    E2_gate -->|否| E2_alert --> E2_action --> E2_end
```

### E-3 旧价报价致合同纠纷（溯源：翻车记录2 + PAIN-002）

```mermaid
flowchart TB
    E3_start(["触发：价格/方案关键变更发布"])
    E3_force["强制: 推送确认阅读通知"]
    E3_check{"全部目标用户已确认？"}
    E3_lock["锁定: 旧版本模板标记失效"]
    E3_end(["结束：旧价风险消除"])
    E3_escalate["升级: 未确认名单报送主管"]

    E3_start --> E3_force --> E3_check
    E3_check -->|是| E3_lock --> E3_end
    E3_check -->|否| E3_escalate --> E3_force
```

### E-4 重复询问同样问题（溯源：翻车记录3 + PAIN-004）

```mermaid
flowchart TB
    E4_start(["触发：销售发布新需求"])
    E4_scan["检测: 相似需求匹配（IN-24）"]
    E4_gate{"存在相似已采纳方案？"}
    E4_suggest["推荐: 历史方案链接"]
    E4_confirm{"销售确认是否满足？"}
    E4_close["关闭: 需求（引用历史方案）"]
    E4_continue["继续: 发布新需求进入正常流程"]
    E4_end(["结束"])

    E4_start --> E4_scan --> E4_gate
    E4_gate -->|是| E4_suggest --> E4_confirm
    E4_confirm -->|是| E4_close --> E4_end
    E4_confirm -->|否| E4_continue --> E4_end
    E4_gate -->|否| E4_continue
```

### E-5 权限配置出错致数据泄漏（溯源：RISK-05）

```mermaid
flowchart TB
    E5_start(["触发：管理员变更权限配置"])
    E5_test["执行: 穿透测试（跨部门数据访问验证）"]
    E5_gate{"测试通过？"}
    E5_apply["生效: 新权限配置"]
    E5_block["阻断: 拒绝生效"]
    E5_rollback["回滚: 恢复上一版本配置"]
    E5_alert["告警: 通知ROLE-03 + STKH-03"]
    E5_end(["结束"])

    E5_start --> E5_test --> E5_gate
    E5_gate -->|通过| E5_apply --> E5_end
    E5_gate -->|未通过| E5_block --> E5_rollback --> E5_alert --> E5_end
```

### E-6 平台空城（溯源：RISK-01 + PAIN-001）

```mermaid
flowchart TB
    E6_start(["触发：周度发布量监控"])
    E6_check["统计: 本周新增商机信息数"]
    E6_gate{"发布量≥阈值？"}
    E6_ok(["正常：平台活跃"])
    E6_alert["告警: 通知ROLE-03运营管理员"]
    E6_action["推动: 管理层推动+纳入KPI/OKR"]
    E6_invite["执行: 运营主动邀约产品线填充内容"]
    E6_end(["结束：空城风险缓解"])

    E6_start --> E6_check --> E6_gate
    E6_gate -->|是| E6_ok
    E6_gate -->|否| E6_alert --> E6_action --> E6_invite --> E6_end
```

### E-7 产品线不响应需求（溯源：RISK-02 + PAIN-004）

```mermaid
flowchart TB
    E7_start(["触发：需求SLA倒计时预警"])
    E7_remind["提醒: 自动催办通知产品线"]
    E7_check{"SLA到期前已响应？"}
    E7_ok(["正常：按时响应"])
    E7_escalate["升级: 触发E-1升级链"]
    E7_record["记录: 响应率纳入产品线考核"]
    E7_end(["结束"])

    E7_start --> E7_remind --> E7_check
    E7_check -->|是| E7_ok
    E7_check -->|否| E7_escalate --> E7_record --> E7_end
```

### 红线溯源汇总表

| 编号 | 红线场景 | 溯源 | 防护机制 |
|------|---------|------|---------|
| E-1 | SLA超时升级链 | RISK-02 + SLA矩阵 | 三级升级（产品线负责人→产品管理部→管理层） |
| E-2 | 新方案发布无人知晓 | 翻车记录1 + PAIN-002 | 48h已读率监控+二次推送 |
| E-3 | 旧价报价致合同纠纷 | 翻车记录2 + PAIN-002 | 强制确认阅读+旧模板锁定 |
| E-4 | 重复询问同样问题 | 翻车记录3 + PAIN-004 | 相似需求检测+历史方案推荐 |
| E-5 | 权限配置出错致数据泄漏 | RISK-05 | 穿透测试阻断+自动回滚 |
| E-6 | 平台空城 | RISK-01 + PAIN-001 | 发布量监控+KPI推动+运营邀约 |
| E-7 | 产品线不响应需求 | RISK-02 + PAIN-004 | SLA倒计时催办+考核纳入 |

---

*文档版本：v1.0 | 渲染日期：2026-05-27 | 节点：/2 Process*
*数据源：research.md（/0 需求调研 v1.0）*
