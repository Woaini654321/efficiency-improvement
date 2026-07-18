# 项目长期记忆 — issue-desk（问题工单平台 / ticket-platform）

最后更新：2026-07-14

## 1. 核心技术栈与约束（永久记住）

- 后端：**Spring Boot 2.7.18 + JDK 1.8 + MongoDB**（spring-data-mongodb）。注意 `javax.*` 而非 `jakarta.*`；无 MyBatis / MySQL / SQL 迁移。
- 前端：**Vue 3 + `<script setup>` + Composition API + TypeScript 严格模式**。禁止 Options API、class 组件。
- UI 库：**Element Plus 2.8**（不是 Ant Design Vue），禁止混用其它 UI 库。
- 状态管理：**无 Pinia**，全局态用模块级 `reactive/ref` 单例（`auth.ts`、`i18n.ts`），可复用逻辑放 `composables/`。
- 包管理：**npm**（`package-lock.json`），不是 pnpm / yarn。
- 认证：**自建 JWT**（无 SSO），Spring Security `hasAuthority(权限码)` 鉴权。
- 响应：Controller 直接返回 DTO / `common/PageResult<T>`，**无 `R<T>` 统一包装**；分页页大小白名单 10/20/50。
- i18n：**强制中英双语**，`frontend/src/i18n.ts` 的 `t(key)`，禁止硬编码文案，localStorage key `ticket.locale`。
- AI：**Hermes Agent**（后端 `hermes/` SSE 透传；前端手写 `fetch`+`ReadableStream`，`api/aiChat.ts`），不是通义千问。`@ai-sdk/vue`/`ai-elements-vue` 已装未用，仅用 `ai` 的 `UIMessage` 类型。
- Commit 规范：**英文 Conventional Commits**（跟随仓库现有历史），`type(scope): 描述`。

## 2. 项目强制工作流程（不可违背）

1. 新需求 / 功能 / 重构 → 先 `superpowers:brainstorming`。
2. 确认方向 → `superpowers:writing-plans` → EnterPlanMode → 等待用户批准。
3. 实施 → 优先 Edit，禁止直接 Write 整文件；改前先 Read。
4. 完成前强制 → `superpowers:verification-before-completion`（build + 前后端测试 + 联调）。
5. 涉及输入 / 鉴权 / 接口 / 密钥 / SQL 审核 → 强制 `security-review`。
6. **严禁** Claude 主动 `git commit` / `git push` / `npm install` / `mvn install` 等高危操作。

## 3. 安全红线（与思政项目相反，务必记牢）

- **本仓库不提交任何明文密钥。** 外部凭据（Archery 密码、飞书 App Secret）AES-GCM 加密入库（`enc:v1:` 前缀），主密钥走环境变量：`TICKET_FEISHU_SECRET_KEY`（docker `:?` 强制、应用惰性校验）、`TICKET_ARCHERY_SECRET_KEY`（仅启用 Archery 时必需）、`HERMES_API_KEY`。
- ⚠️ `TICKET_FEISHU_ALLOW_LOCAL_SECRET_KEY=true` 会回退到源码内置弱密钥——仅本地开发，生产严禁。
- 失败原因必须脱敏：密码 / JWT / Authorization 头 / 完整 SQL 不回显给前端或日志。
- 外部 URL 只允许 http/https，不得含用户名密码/查询参数/片段；`verifyTls` 默认 true。
- **AI（Hermes）结论仅为建议**，状态迁移与审批必须人工确认，AI 不得自动推进工单。
- **Archery 与飞书集成均默认关闭。Archery SQL 审核：绝不自动审批、绝不自动执行 SQL**，只提交工单和观察状态。仅 `FAILED` 且无 Archery 工单号可由 `ticket:sql-review` 权限工程师重试，`TP-xxxxxxxx` 幂等 marker，多匹配保持 `FAILED` 要求人工确认。
- 发现安全问题（XSS、注入、未验证输入、密钥泄漏、SSRF）必须立即指出并给修复方案。

## 4. RBAC 权限码（改鉴权前先对照）

三角色：普通用户 / 工程师 / 管理员（演示密码 `password123`）。菜单由 `GET /api/menus/my` 按权限动态返回。

权限码全集以 `auth/SecurityConfig.java` + `auth/DemoIdentityInitializer.java` 为准，高频项：`ticket:{submit,read:self,read:all,operate,claim,resolve,sql-review,feishu-notify,hermes-agent,ai-solution,process-log,confirm-type,manual-task,engineer-urgency,refinement,requirement-status}`、`config:manage` + `config:{system,module,dict,template}:{view,save}` + `config:feishu:save`、`rbac:{manage,user:save,role:save,menu:save}`、`stats:read`。

后端 `hasAuthority(...)` 与前端 `router.ts` 的 `meta.permission` 必须一致。前端 `auth.ts` 的 `hasPermission()` 对 `config:*` 有 `config:manage` 兜底放行。

## 5. 多 Agent 协作约定

详见 `AGENTS.md` 角色分工表。API / 权限码 / 响应格式变更必须 @Architect @Backend @Frontend @QA。

## 6. 常用路径速查

**后端（`backend/src/main/java/com/example/ticket/`）**
- 工单：`ticket/`
- AI：`ai/` + `hermes/`（Hermes SSE 透传）
- 配置中心：`configcenter/`（系统/模块/字典/模板/Archery/飞书配置）
- 认证 RBAC：`auth/`（JWT、用户/角色/菜单）
- 集成：`integration/` + `archery/` + `feishu/`
- 单号：`sequence/`（`IT-yyyyMMdd-NNNN`；`TP-xxxxxxxx` 是 Archery 幂等 marker，非单号）
- 公共：`common/`（PageResult / PageRequestParams）
- 配置：`backend/src/main/resources/application.yml`

**前端（`frontend/src/`）**
- 入口：`main.ts`；路由：`router.ts`；鉴权：`auth.ts`
- 页面：`views/` + `views/config/`
- AI 组件：`components/ai/`
- API 契约层：`api/`
- 复用逻辑：`composables/`；i18n：`i18n.ts`

## 7. 构建与运行速查

- 后端打包：`cd backend && mvn -q -DskipTests -Dmaven.repo.local=../.m2/repository package`
- 后端测试：`cd backend && mvn -q -Dmaven.repo.local=../.m2/repository test`
- 起后端 + Mongo：`docker compose -p ticket-platform up --build -d mongodb backend`
- 前端 dev：`cd frontend && npm run dev -- --host 127.0.0.1`
- 前端 build：`cd frontend && npm run build`；前端 test：`cd frontend && npm run test`
- 改 Java 代码后**必须重打 jar + 重建后端容器**，只刷页面不生效。

## 8. 最后提醒

所有 Claude 的文件操作（Write/Edit）需用户手动确认权限。所有 git 写操作（commit/push）由用户自己执行，Claude 只允许 `git status/diff/log/show`。
