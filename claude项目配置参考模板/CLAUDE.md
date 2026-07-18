# CLAUDE.md — 问题工单平台（issue-desk / ticket-platform）

最后更新：2026-07-14

> 本文件是本仓库对 Claude Code 的强制规则，优先级高于默认行为。
> 全局个人偏好仍适用，但**本文件与全局偏好冲突时，以本文件为准**（本项目技术栈、安全姿态与思政项目差异很大）。

---

## 1. 回复与沟通

- 所有回复使用中文，专业、简洁、高效。
- 不使用表情符号，除非我明确要求。
- 需求不明确时先用 AskUserQuestion 澄清，不要猜测。
- 修改文件前先说明计划、等待确认；每次改动控制在最小范围；改完简要说明改了什么、为什么。

## 2. 项目概况

工单平台，含**两条产品线**（不要只当成"问题单"）：
- **问题单主线**：提交 → 类型确认（可 AI 生成）→ 工程师确认 → 处理（可含 SQL 审核子流程）→ 用户确认 → 解决 → 关闭。
- **需求单 track**：需求排期 → 开发确认 → 开发中 → 已发布。
- 贯穿能力：Hermes AI 辅助、外部集成（飞书通知 / Archery SQL 审核）、RBAC 权限与后台配置。

- 后端：`backend/`，Spring Boot 2.7.18 + JDK 1.8 + MongoDB
- 前端：`frontend/`，Vue 3 + TypeScript + Vite + Element Plus
- 本地运行：Docker 跑 MongoDB + 后端，本机 Node 跑前端；详见 `README.md`
- 多 Agent 协作角色与硬规则见 `AGENTS.md`；长期记忆见 `MEMORY.md`

### 2.1 工单状态机（`ticket/TicketStatus.java`，共 8 态）

> 2026-07-15 精简：原声明 16 态但生产只驱动 8 态，已删除 8 个死态（`SUBMITTED/CONFIRMING/AI_GENERATING/ENGINEER_CONFIRMING/SQL_REVIEWING/SQL_DONE/SQL_FAILED/CLOSED`）与 3 个死可见态（`SUBMITTED/SOLUTION_GENERATING/CLOSED`）。**`RESOLVED` 为唯一终态**。设计/计划见 `docs/superpowers/{specs,plans}/2026-07-15-ticket-status-8-state-simplification*.md`。

```
问题单：PENDING_CONFIRM → PROCESSING → WAITING_USER_CONFIRMATION →（验收通过）RESOLVED
需求单：REQUIREMENT_SCHEDULING → REQUIREMENT_DEV_CONFIRMING → REQUIREMENT_DEVELOPING
        → REQUIREMENT_RELEASED → WAITING_USER_CONFIRMATION →（验收通过）RESOLVED
用户驳回（WAITING_USER_CONFIRMATION，见 confirmResolution，须填驳回理由）：
        问题单 →（退回）PROCESSING；需求单 →（打回重开发）REQUIREMENT_DEVELOPING
```

- **需求线非终态说明**：`REQUIREMENT_RELEASED`（已发布）**不是终态**，`RESOLVED` 为唯一终态；已发布需求单须经提交人验收（→ `WAITING_USER_CONFIRMATION`）才终结，期间仍属活跃工单（PM 2026-07-15 确认，"发布≠完结"）。
- **合法迁移规则以 `ticket/TicketStatusPolicy` 为准，"用户可见状态 ≠ 内部状态"的映射（4 个可见态）见其 `toUserVisibleStatus`**（提交人/工程师/管理员看到的状态不同）。改动状态流转前先读 Policy 与 `TicketService`，不要凭本图臆断迁移条件。
- **SQL 审核 / AI 方案进度不再是工单态**：分别落在 `IntegrationTaskDocument`/`AiChatRunDocument` 子文档与流程日志，工单状态在此期间恒为 `PROCESSING`。Archery/SQL 红线见第 7 节。

## 3. 技术栈基准（务必记准，勿套用思政项目）

| 层 | 技术 | 说明 |
|---|---|---|
| 后端框架 | **Spring Boot 2.7.18** | 不是 3.x；注意 `javax.*` 而非 `jakarta.*` |
| JDK | **Java 8（1.8）** | 无 records/sealed；高版本语法糖需先确认 |
| 持久层 | **MongoDB**（spring-data-mongodb） | 文档库，非关系型；无 MyBatis、无 MySQL、无 SQL 迁移 |
| 认证 | **自建 JWT**（`auth/JwtService` + `JwtAuthenticationFilter`） | 无 SSO；Spring Security `hasAuthority(权限码)` 鉴权 |
| 分页 | `common/PageResult<T>` + `PageRequestParams` | 页大小白名单 10/20/50，默认 10 |
| 响应 | Controller 直接返回 DTO / `PageResult<T>` | **无 `R<T>` 统一包装**，不要臆造 |
| HTTP 客户端 | Apache HttpClient | 外部集成调用 |
| 构建 | Maven | 详见第 6 节命令 |
| 前端框架 | **Vue 3 + `<script setup>` + Composition API** | 禁止 Options API、禁止 class 组件 |
| UI 库 | **Element Plus 2.8** | 禁止混用其它 UI 库（不是 Ant Design Vue） |
| 状态 | **无 Pinia** | 全局态用模块级 `reactive/ref` 单例（`auth.ts`、`i18n.ts`）；可复用逻辑放 `composables/` |
| 路由 | Vue Router 4，`frontend/src/router.ts` | 路由 `meta.permission` 权限码 + 动态菜单过滤 |
| 包管理 | **npm**（`package-lock.json`） | 不是 pnpm，不是 yarn |
| i18n | 自建 `frontend/src/i18n.ts` 的 `t(key)` | **强制中英双语**，见第 9 节 |
| AI | **Hermes Agent**（后端 `hermes/` SSE 透传） | 前端用手写 `fetch`+`ReadableStream` 消费（`api/aiChat.ts`）；`@ai-sdk/vue`/`ai-elements-vue` 为已装未用依赖，仅用 `ai` 的 `UIMessage` 类型。不是通义千问 |
| 测试 | 后端 JUnit + Testcontainers(Mongo)；前端 Vitest + `@vue/test-utils` | 仓库已有大量 `*.spec.ts`/`*.spec.js`，新增/改动须配套测试 |
| Mongo 索引 | `application.yml` `auto-index-creation: true` | 索引由实体 `@Indexed`/`@CompoundIndex` 注解自动创建，改字段可能改索引 |

**后端模块速查（`backend/src/main/java/com/example/ticket/`）：**
`ticket`（工单生命周期）· `ai` + `hermes`（AI 会话/解决方案）· `configcenter`（系统/模块/字典/模板/其它配置）· `auth`（JWT + RBAC 用户/角色/菜单）· `integration` + `archery`（SQL 审核接入）· `feishu`（消息推送）· `sequence`（`IT-yyyyMMdd-NNNN` 单号）· `stats`（统计）· `common`（分页等）

> `TP-xxxxxxxx` **不是**单号，是 Archery 工单幂等 marker（取自集成任务 id 前 8 位），只出现在 Archery 集成，见第 7 节。

**前端关键路径（`frontend/src/`）：**
- 页面：`views/`（SubmitIssue / MyTickets / EngineerQueue / AdminTickets / PermissionCenter / `config/*`）
- AI 会话：`components/ai/`；其它共享组件：`components/`（`SystemSelect`/`ModuleSelect`/`UserSelect`/`TicketDetailDialog` 等）
- 类型：`types.ts`（全局领域类型）、`types/aiChat.ts`（AI 会话类型）—— 类型统一放这里
- 请求：普通 JSON 走 `api/tickets.ts` 的 `request<T>()`（自动注入 `Authorization`/`Content-Type`）；**流式 SSE 走 `api/aiChat.ts` 的导出函数 `startAiChatStream`/`subscribeAiChatRun`（内部 `consumeAiChatStream` 手写 `fetch`+`ReadableStream`；独立通道，勿塞进 `request()`）**
- 其它：`composables/` · `i18n.ts` · `router.ts` · `auth.ts`（`hasPermission`/鉴权态）
- localStorage key：`ticket.locale`（语言）、`ticket.auth.token`、`ticket.auth.user`

## 4. 代码风格

**前端（Vue3 + TS）**
- 只用 `<script setup>` + Composition API；`ref/reactive/computed/toRefs`。
- 严格模式 TS；组件 PascalCase，变量/函数 camelCase，常量 UPPER_CASE。
- 字符串单引号、末尾不加分号。
- 组件文件 PascalCase（`TicketDetailDialog.vue`），工具函数 camelCase（`markdown.ts`）。
- 用 ES Module（import/export），禁止 CommonJS。
- 不在 `<script setup>` 直接操作 DOM，用 template ref。
- API 调用统一走 `frontend/src/api/*`，不要在组件里散写 fetch：普通 JSON 用 `tickets.ts` 的 `request()`，流式用 `aiChat.ts`。
- 状态/枚举文案优先用 `i18n.ts` 的 `menuName/statusLabel/issueTypeText/aiStatusText/sqlStatusText/actionText`，普通文案用 `t(key)`，避免漏译。
- 避免不必要的注释、类型守卫、防御性 try/catch（信任框架边界）。

**后端（Java 8 + Spring Boot 2.7）**
- 标准 Java 命名（类 PascalCase，方法/变量 camelCase，常量 UPPER_CASE）。
- 分层：Controller → Service → Repository（Spring Data Mongo）。
- 权限用 `@PreAuthorize` / `hasAuthority("<权限码>")`，权限码见第 8 节。
- 日志用 SLF4J，禁止 `System.out.println`；**外部集成的密钥/JWT/Authorization 头/完整 SQL 绝不写入日志**（见第 7 节）。
- 注意是 `javax.*`（Spring Boot 2.7），别写成 `jakarta.*`。

## 5. 工作流程（强制）

- 本项目使用 Superpowers 插件，纪律优先于直觉，分阶段隔离上下文工作。
- 多文件变更、新功能、架构调整 → **先 EnterPlanMode**，输出清晰步骤 + 风险点，经我确认后再执行。
- 修改前必须先 Read 文件，不要猜测代码。
- 优先用 Edit 精准替换，不要整文件 Write。
- 标准链路：`需求 → brainstorming → writing-plans → 实施 → verification-before-completion → 完成`。

## 6. 构建与运行命令

> 只在需要验证时运行读/构建/测试命令；**禁止**执行安装、部署、git 写操作等高危命令（第 12 节）。

- 后端打包：`cd backend && mvn -q -DskipTests -Dmaven.repo.local=../.m2/repository package`
- 后端测试：`cd backend && mvn -q -Dmaven.repo.local=../.m2/repository test`
- 起/重启 Mongo + 后端：`docker compose -p ticket-platform up --build -d mongodb backend`
- 前端开发：`cd frontend && npm run dev -- --host 127.0.0.1`（`--host 127.0.0.1` 覆盖 `package.json` 默认的 `0.0.0.0`）
- 前端构建（含类型检查）：`cd frontend && npm run build`（= `vue-tsc --noEmit && vite build`）
- 前端测试：`cd frontend && npm run test`（Vitest）
- 重要：改了 Java 代码后，**必须重新打包 jar 并重建后端容器**，只刷页面不会生效。
- 前端 dev 代理：`vite.config.ts` 把 `/api` 代理到 `VITE_PROXY_TARGET`（默认 `http://localhost:8080`）。
- `docker-compose.yml` 另含**可选** `frontend` 服务；本地推荐只起 `mongodb backend`，前端走本机 Node（热更新更快）。
- 演示账号：`user001` / `engineer001` / `admin`，密码均 `password123`（`auth/DemoIdentityInitializer.java`）。

**常用环境变量（默认值来源见 `application.yml` / `docker-compose.yml`）：**
- 密钥：`TICKET_FEISHU_SECRET_KEY`、`TICKET_ARCHERY_SECRET_KEY`、`HERMES_API_KEY`（见第 7 节）
- Hermes：`HERMES_BASE_URL`(:8642)、`HERMES_MODEL`(hermes-agent)、`HERMES_PREFERRED_API`(responses)、`HERMES_TIMEOUT_SECONDS`(180)
- 飞书：`FEISHU_TOKEN_URL`/`FEISHU_MESSAGE_URL`/`FEISHU_CONNECT_TIMEOUT_MS`(3000)/`FEISHU_READ_TIMEOUT_MS`(10000)
- 调度 tick：`ARCHERY_SYNCHRONIZER_TICK_MS`(15000)；飞书 outbox/对账与 AI run 维护周期是各自 `@Scheduled` 占位符默认值（可用同名 property 覆盖）：`ticket.feishu.dispatcher-delay-ms`(5000)、`ticket.feishu.reconciliation-delay-ms`(30000)、`ai.chat.run-maintenance-ms`(10000)
- 数据库：`MONGODB_URI`(mongodb://localhost:27017/ticket_platform)

## 7. 安全与凭据（本项目红线，与思政项目相反）

**本仓库不提交任何明文密钥。** 思政项目"内部仓库可直接提交凭据"的规则在此**不适用**。

- 外部凭据（Archery API 密码、飞书 App Secret）以 **AES-GCM 加密**（`enc:v1:` 前缀，SHA-256 派生密钥）存入 MongoDB；加密主密钥只走环境变量：
  - `TICKET_FEISHU_SECRET_KEY`：`docker-compose.yml` 用 `:?` 语法在启动时强制存在；应用侧是**惰性校验**（仅在加解密飞书 secret 时才失败），不是启动即校验。
  - `TICKET_ARCHERY_SECRET_KEY`：**仅在启用 Archery 时才必需**（默认关闭时可不设）。
  - `HERMES_API_KEY`。
- ⚠️ 存在本地绕过开关 `TICKET_FEISHU_ALLOW_LOCAL_SECRET_KEY`（默认 `false`）：开启后回退到**源码内置弱密钥**，**仅限隔离的本地开发/测试，生产严禁开启**（生产必须外部注入独立主密钥）。
- 主密钥不得写入源码、日志、镜像；旋转主密钥前先关闭对应集成，换密钥后需重新录入 App Secret / API 密码。
- 失败原因必须脱敏：**密码、JWT、Authorization 头、完整 SQL 一律不回显**给前端或日志（`application.yml` 已关闭 httpclient headers/wire 日志）。
- 外部 URL 只允许 http/https，不得含用户名/密码、查询参数或片段；`verifyTls` 默认 `true`，生产 HTTPS 必须保持开启。
- 发现安全问题（XSS、注入、未验证输入、密钥泄漏、SSRF 等）→ 立即指出并给出修复方案。
- 涉及用户输入 / 鉴权 / 外部接口 / 密钥 / 数据库操作时，主动调用 `security-review`。

**AI（Hermes）红线：** Hermes 生成的类型判定、解决方案、SQL 结论**一律为建议**，最终的状态迁移与审批**必须人工确认**，AI 不得自动推进工单或替代人工审批。

**Archery SQL 审核绝对红线：**
- 本平台**绝不自动审批、绝不自动执行 SQL**，只提交工单和观察状态。
- Archery 与飞书集成**均默认关闭**；关闭时新任务走本地人工流程 / 不推送。
- 只有 `FAILED` 且尚无 Archery 工单号的任务可由 `ticket:sql-review` 权限工程师重试；重试用 `TP-xxxxxxxx` 幂等 marker 查找，多匹配时保持 `FAILED` 要求人工确认。

## 8. RBAC 权限码（改动鉴权前先对照）

三角色：普通用户 / 工程师 / 管理员（演示密码 `password123`）。左侧菜单由 `GET /api/menus/my` 按权限动态返回。

> **权限码全集以 `auth/SecurityConfig.java` + `auth/DemoIdentityInitializer.java` 为准，下表仅按域列高频项，不是全集。** 新增接口/菜单时以后端源码对照，不要把此表当成完整清单。

- **工单**：`ticket:submit`、`ticket:read:self`、`ticket:read:all`、`ticket:operate`、`ticket:claim`、`ticket:resolve`、`ticket:sql-review`、`ticket:feishu-notify`、`ticket:hermes-agent`、`ticket:ai-solution`、`ticket:process-log`、`ticket:confirm-type`、`ticket:manual-task`、`ticket:engineer-urgency`、`ticket:refinement`、`ticket:requirement-status`
- **配置**：`config:manage`、`config:system:view`/`:save`、`config:module:view`/`:save`、`config:dict:view`/`:save`、`config:template:view`/`:save`、`config:feishu:save`
- **RBAC**：`rbac:manage`、`rbac:user:save`、`rbac:role:save`、`rbac:menu:save`
- **统计**：`stats:read`

- 新增受保护接口时，后端 `hasAuthority(...)` 与前端 `router.ts` 的 `meta.permission` **必须一致**。
- 前端有兜底规则：`auth.ts` 的 `hasPermission()` 对任意 `config:*` 权限，只要用户具备 `config:manage` 即放行——改配置类鉴权时留意此规则。
- ⚠️ **菜单可见性与接口鉴权耦合**：用户最终权限 = `role.permissions` **∪ 可见菜单的 `menu.permission`**（`SecurityUser.getAuthorities()`，仅 `visible=true` 的菜单计入）。像 `ticket:claim`/`ticket:resolve` 这类按钮级权限**只由可见菜单派生**、未写入角色 `permissions`；因此在权限中心把对应菜单 `visible` 置为 false，会**同时**让前端按钮消失和后端接口 403。改菜单可见性前须知晓此隐性契约。
- 权限码/菜单变更属于契约变更，须通知 Architect（见 `AGENTS.md`）。

## 9. 国际化（强制）

- 前端必须支持简体中文 + 英文。新增的页面、菜单、按钮、表格列、表单标签、占位符、弹窗标题、校验信息、toast、空状态**一律**用 `i18n.ts` 的 `t(key)`，禁止硬编码展示文案。
- 应用外壳与登录页必须有可见的语言切换器；所选语言持久化（localStorage `ticket.locale`）并在刷新后恢复。
- 后端业务数据（工单内容、系统名、模块名、模板字段名）可按后端返回原样显示；菜单标签优先按稳定 menu code 翻译，后端标签兜底。

## 10. 前端布局约束（详见 `docs/frontend-development-constraints.md`）

- 登录后浏览器页面不作为主滚动容器；侧栏/头部/导航固定，内容在工作区内部滚动。
- 可纵向增长的组件，其 flex/grid 父级须设 `min-height: 0`。
- 每个数据列表必须分页，分页属于列表容器而非浏览器页面。
- 弹窗/抽屉定义相对视口的最大高度，长内容在其 body 内部滚动，页脚主操作始终可达。
- 布局改动需在桌面宽度与窄屏各验证一次，并验证中英文切换后主流程可用。

## 11. Git 操作策略（严格限制）

- **禁止** Claude 自动执行 `git commit`、`git push`、`git rebase`、`git reset` 等写操作。
- 允许：`git status`、`git diff`、`git log`、`git show`。
- 变更就绪时用中文提醒，例如："文件已修改完成，可手动执行 `git add . && git commit -m '…'`"。
- **Commit 规范：英文 Conventional Commits**，跟随本仓库现有历史：`type(scope): 描述`，类型取 `feat/fix/refactor/docs/test/style/perf/chore`，首行 < 50 字符（例：`fix: lock configuration refresh during saves`）。

## 12. 永远禁止（红线）

- 执行 `npm/yarn/pnpm install`、`mvn install`/`mvn deploy`、`git push`、`git reset --hard`、`rm -rf` 等高危命令。
- 引入 `package.json` / `pom.xml` 之外的新依赖（须我明确同意）。
- 提交任何明文密钥到仓库（见第 7 节）。
- 未通知 Architect 就修改接口路径、响应字段或权限码。

## 13. 必须使用的 Skills

| 场景 | Skill |
|---|---|
| 新功能 / 需求变更 / 技术选型 / 重构前 | `superpowers:brainstorming` |
| 进入实施前 | `superpowers:writing-plans` → EnterPlanMode |
| 遇到 bug / 测试失败 / 异常行为 | `superpowers:systematic-debugging` |
| 声称"完成"前 | `superpowers:verification-before-completion` |
| 新增功能 / 修 bug / 重构 | `superpowers:test-driven-development`（红-绿-重构） |
| 涉及输入 / 鉴权 / 接口 / 密钥 / 数据库 | `security-review` |

## 14. Memory 习惯

- 架构决策、反复出现的偏好、外部集成约定 → 写入 `MEMORY.md`。
- 临时任务细节不写入长期记忆。
- 重要结论后可建议："是否记录到 MEMORY.md？"

用 AskUserQuestion 随时澄清需求。
