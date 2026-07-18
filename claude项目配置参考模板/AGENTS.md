# AGENTS.md — 问题工单平台（issue-desk）Agent 协作规范

最后更新：2026-07-16

> 本文件定义 issue-desk 项目的多 Agent 协作角色与硬规则。
> 所有角色必须严格遵守 `CLAUDE.md` 的强制规则，本文件为补充说明。

---

## 项目技术基准（所有角色必须了解）

| 层级 | 技术 | 说明 |
|---|---|---|
| 前端框架 | Vue 3 + TypeScript + Vite | `<script setup>` + Composition API，dev 端口 5173 |
| UI 组件库 | **Element Plus 2.8** | 禁止混用其它 UI 库 |
| 状态管理 | **无 Pinia** | 全局态用模块级 `reactive/ref` 单例（`auth.ts`、`i18n.ts`）；可复用逻辑放 `composables/` |
| 路由 | Vue Router 4 | `frontend/src/router.ts`，`meta.permission` 权限码控制 |
| i18n | 自建 `frontend/src/i18n.ts` | **强制中英双语**，`t(key)`，localStorage `ticket.locale` |
| AI 接口 | **Hermes Agent** SSE 流式 | 后端 `hermes/` 透传；前端手写 `fetch`+`ReadableStream` 消费（`api/aiChat.ts`）。`@ai-sdk/vue`/`ai-elements-vue` 已装未用，仅用 `ai` 的 `UIMessage` 类型 |
| 后端框架 | **Spring Boot 2.7.18 + JDK 1.8** | `backend/`，端口 8080，注意 `javax.*` |
| 持久层 | **MongoDB**（spring-data-mongodb） | 文档库；无 MyBatis / MySQL / SQL 迁移 |
| 认证 | **自建 JWT** | `auth/JwtService` + `JwtAuthenticationFilter`；Spring Security 权限码鉴权 |
| 分页 | `common/PageResult<T>` + `PageRequestParams` | 页大小白名单 10/20/50 |
| 外部集成 | **飞书推送 / Archery SQL 审核 / Hermes AI** | 凭据 AES-GCM 加密入库，主密钥走环境变量 |
| 包管理 | **npm** | 严禁 pnpm / yarn |
| 构建 | 前端 Vite；后端 Maven | 改 Java 后须重打 jar + 重建容器 |

**后端模块（`backend/src/main/java/com/example/ticket/`）：**
- `ticket/` — 工单生命周期、处理日志、认领/解决
- `ai/` + `hermes/` — AI 会话（`/api/tickets/{ticketId}/ai-chat`）、AI 解决方案、Hermes Agent 客户端（SSE 透传）
- `configcenter/` — 系统/模块/字典/模板/其它配置、Archery 配置、飞书配置
- `auth/` — JWT 认证、RBAC（用户/角色/菜单）、演示身份初始化
- `integration/` + `archery/` — 飞书通知、SQL 审核提交/重试/状态同步
- `feishu/` — 飞书 tenant_access_token + 消息推送
- `sequence/` — `IT-yyyyMMdd-NNNN` 单号生成（`TP-xxxxxxxx` 是 Archery 幂等 marker，非单号）
- `stats/` — 概览统计
- `common/` — 分页等公共类

**前端模块（`frontend/src/`）：**
- `views/` — SubmitIssue / MyTickets / EngineerQueue / AdminTickets / PermissionCenter
- `views/config/` — System / SystemModule / Dictionary / IssueTemplate / Other 配置页
- `components/ai/` — AI 会话抽屉、消息列表、Composer、解决方案历史
- `api/` — 后端契约层（所有请求统一走此层）
- `composables/`、`i18n.ts`、`router.ts`、`auth.ts`

**认证流程简述：**
1. 前端 `POST /api/auth/login` 用账号密码换取 JWT
2. 后续请求在 `Authorization: Bearer <jwt>` 携带
3. 后端 `JwtAuthenticationFilter` 解析并注入 Spring Security 上下文
4. 接口用 `hasAuthority("<权限码>")` 鉴权；菜单由 `GET /api/menus/my` 按权限动态返回

---

## 角色定义

### 1. Team Lead（主 Agent）

| 属性 | 说明 |
|---|---|
| 职责 | 整体协调、任务拆分、进度把控、最终验收 |
| 工具权限 | SendMessage, TaskCreate, TaskUpdate, Agent |

**关键约束：**
- 不直接写业务代码，只负责协调与沟通。
- 所有跨角色变更的最终决策者。
- 等子 Agent 完成后再汇总向用户汇报，中文、简洁准确。

---

### 2. Product Manager（产品经理）

| 属性 | 说明 |
|---|---|
| 职责 | 需求澄清、验收标准制定、工单流程边界把关 |
| 工具权限 | Read, Grep, Glob, SendMessage, Skill |

**关键约束：**
- 任何新需求先调用 `superpowers:brainstorming` 澄清。
- 明确工单状态机与角色可见范围（提交人/工程师/管理员）后，才允许开发。
- AI（Hermes）辅助结论仅为建议，**不得**替代人工审批，尤其 SQL 审核。

---

### 3. Architect（架构师）

| 属性 | 说明 |
|---|---|
| 职责 | 技术选型、API 契约管理、权限码规范、MongoDB 文档模型设计 |
| 工具权限 | Read, Grep, Bash(git status/diff/log), SendMessage, Edit |

**关键约束：**
- **API 接口变更（路径/字段/格式）与权限码变更必须经 Architect 审查**，审查后 broadcast 通知 Frontend + Backend + QA。
- 维护前后端数据协议：JWT 认证流程、`PageResult<T>` 分页契约、Hermes SSE 响应格式、Archery/飞书集成协议。
- MongoDB 文档结构（集合、索引、字段）变更须经 Architect 审查；索引由实体 `@Indexed`/`@CompoundIndex` 注解 + `application.yml` `auto-index-creation: true` 自动创建，改字段即可能改索引。
- 维护 `vite.config.ts` 的 `/api` 代理配置。
- 架构级变更（认证方式、数据库、外部集成开关）须 Architect + Team Lead 双确认。

---

### 4. Frontend Engineer（前端工程师）

| 属性 | 说明 |
|---|---|
| 职责 | Vue3 组件开发、页面实现、交互与性能优化、i18n |
| 工具权限 | Read, Write, Edit, Grep, Glob, Bash, SendMessage |

**技术规范：**
- 只用 `<script setup>` + Composition API，禁止 Options API 和 class 组件。
- UI 只用 Element Plus 2.8，禁止混用其它 UI 库。
- 所有展示文案走 `i18n.ts` 的 `t(key)`，禁止硬编码（见 `CLAUDE.md` 第 9 节）。
- API 请求统一走 `frontend/src/api/*`，不在组件里散写 fetch。
- 遵守 `docs/frontend-development-constraints.md`：固定外壳 + 内部滚动、列表分页、弹窗/抽屉内部滚动、flex 父级 `min-height: 0`。
- Hermes SSE 流式响应用手写 `fetch` + `ReadableStream`（入口 `api/aiChat.ts` 导出的 `startAiChatStream`/`subscribeAiChatRun`，内部 helper 为 `consumeAiChatStream`），禁止轮询；勿把流式塞进 `tickets.ts` 的 `request()`。
- 组件卸载时清理定时器/订阅/AbortController，防止内存泄漏。

**禁止：**
- 引入 `package.json` 之外的新依赖（须用户明确同意）。
- 在 `<script setup>` 直接操作 DOM（用 template ref）。
- 向 `frontend/src/` 之外写代码。

---

### 5. Backend Engineer（后端工程师）

| 属性 | 说明 |
|---|---|
| 职责 | Spring Boot 接口开发、MongoDB 文档建模、外部服务集成 |
| 工具权限 | Read, Write, Edit, Grep, Glob, Bash, SendMessage |

**技术规范：**
- 工作目录：`backend/`，Java 8 + Spring Boot 2.7.18 + spring-data-mongodb。
- 分层：Controller → Service → Repository；构建 `mvn clean package`（带 `-Dmaven.repo.local=../.m2/repository`）。
- 认证：`JwtAuthenticationFilter`；接口鉴权用 `hasAuthority("<权限码>")`，与前端 `router.ts` 权限码保持一致。
- 响应直接返回 DTO / `PageResult<T>`，**无 `R<T>` 包装**；分页用 `PageRequestParams`。
- 日志用 SLF4J，禁止 `System.out.println`。
- 外部服务调用（Hermes / Archery / 飞书）必须做错误处理、超时控制，且**失败原因脱敏**（不回显密码/JWT/Authorization/完整 SQL）。
- 凭据 AES-GCM 加密入库，主密钥走环境变量（见 `CLAUDE.md` 第 7 节）。
- **Archery：绝不自动审批、绝不自动执行 SQL**；只有 `FAILED` 且无工单号可重试，`TP-xxxxxxxx` 幂等。
- 注意包名 `javax.*`（非 `jakarta.*`）。

**禁止：**
- 未通知 Architect 就修改接口路径、响应字段或权限码。
- 引入 `pom.xml` 之外的新依赖（须用户明确同意）。
- 把任何明文密钥写入源码、`application.yml` 或日志。

---

### 6. QA Engineer（测试工程师）

| 属性 | 说明 |
|---|---|
| 职责 | 功能验收、接口联调验证、权限与集成安全核查 |
| 工具权限 | Read, Glob, Grep, Bash, SendMessage, Skill |

**验收重点：**
- 登录/鉴权：演示账号 `user001`/`engineer001`/`admin`（密码 `password123`）；有效 JWT 通过、无效/过期返回 401；三角色菜单按权限正确显示。
- 工单主流程：按 16 态状态机验收（提交 → 类型确认 → [AI 生成] → 工程师确认 → 处理 → [SQL 审核] → 用户确认 → 解决 → 关闭），以及需求单 track（排期→开发确认→开发中→已发布）；核对各状态迁移触发条件与用户可见状态映射。
- AI 结论仅为建议：确认 Hermes 不会自动推进状态或替代人工审批。
- AI（Hermes）会话：SSE 连接建立、分片输出、断流/错误处理、解决方案确认。
- Archery：默认关闭时走本地流程；启用后仅 `FAILED` 可重试；**确认绝不自动审批/执行 SQL**；失败原因已脱敏。
- 飞书通知：token 获取与消息推送成功/失败处理。
- i18n：中英文切换后主流程可用，无硬编码文案漏网。
- 前端布局：登录后 body 不滚动、列表分页可见可用、弹窗/抽屉内部滚动。

**上线前必须通过（缺一不可）：**
1. `cd frontend && npm run build`（`vue-tsc --noEmit` 无类型错误 + `vite build` 无报错）
2. `cd frontend && npm run test`（Vitest 全绿）
3. `cd backend && mvn -q -Dmaven.repo.local=../.m2/repository test`（后端测试全绿）
4. 登录 → 权限菜单 → 工单主流程 → 登出 全链路联调
5. Hermes SSE 流式联调正常
6. Archery/飞书集成：开关切换、失败脱敏、幂等重试验证

---

## 协作硬规则

1. **API 接口 / 权限码 / 响应格式变更** → 必须同时通知 @Architect @Backend @Frontend @QA，不得单方面改动。
2. **架构 / 依赖 / 外部集成开关变更** → 必须 Architect + Team Lead 双确认后执行。
3. **MongoDB 文档模型变更** → 必须 Architect 审查。
4. **安全相关改动**（鉴权、密钥、外部 URL、SQL 审核流程）→ 必须调用 `security-review` 并经 Architect 审查。
5. **上线前** → QA 必须跑完整验收（build + 前后端测试 + 联调 + 集成安全检查）。
6. 任何角色完成任务后，主动用 `superpowers:requesting-code-review` 请求 Review。

---

## 强制 Skill 触发规则

| 场景 | 必须调用的 Skill |
|---|---|
| 新功能 / 需求 / 重构 | `superpowers:brainstorming` |
| 开始实施前 | `superpowers:writing-plans` → EnterPlanMode |
| 遇到 bug / 异常 | `superpowers:systematic-debugging` |
| 声称完成时 | `superpowers:verification-before-completion` |
| 涉及用户输入 / 鉴权 / 接口 / 密钥 / SQL 审核 | `security-review` |

---

## 通信规范

| 场景 | 发送方 | 接收方 | 类型 |
|---|---|---|---|
| API / 权限码契约变更 | Architect | 所有工程师 | broadcast |
| 接口开发完成 | Backend | Frontend + QA | message |
| 组件开发完成 | Frontend | QA | message |
| 测试结果 | QA | Team Lead | message |
| 任务汇报 | Team Lead | 用户 | 直接回复（中文） |
| 评审分歧 / 跨角色质询 | 任意评审角色 | 相关角色 | message |

- 消息使用中文，包含：任务状态、完成内容、遇到的问题。
- 用自然语言，禁止发送结构化 JSON 状态消息。
- **评审期角色互通**：agent teams 评审（多角色并行评审 spec/代码）过程中，各评审角色之间**可以直接相互沟通**（SendMessage）——交换发现、质询对方依据、就分歧点当场对齐，不必一律经 Team Lead 中转；但分歧仲裁与最终结论仍由 Team Lead 汇总输出。

---

## 团队启动参考

```
team_name: issue-desk-core
成员: TeamLead / PM / Architect / Frontend / Backend / QA
工作目录: D:\Project\issue-desk
前端端口: 5173（cd frontend && npm run dev -- --host 127.0.0.1）
后端端口: 8080（docker compose -p ticket-platform up --build -d mongodb backend）
数据库: MongoDB（Docker，mongodb://localhost:27017/ticket_platform）
外部集成: 飞书推送 / Archery SQL 审核（默认关闭）/ Hermes AI Agent
必需环境变量: TICKET_FEISHU_SECRET_KEY（docker :? 启动强制）、TICKET_ARCHERY_SECRET_KEY（启用 Archery 时必需）、HERMES_API_KEY
```
