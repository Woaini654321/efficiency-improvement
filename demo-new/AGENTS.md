# AGENTS.md — 销售商机互助平台（sales-lead-hub）多 Agent 协作规范

最后更新：2026-07-18

> 本文件定义 sales-lead-hub 的多 Agent 协作角色与硬规则。
> 所有角色必须严格遵守 `CLAUDE.md`（尤其第 0 节「黑盒纪律」与第 4 节「env 白名单」），本文件为补充。

---

## 项目技术基准（所有角色必须了解）

| 层级 | 技术 | 说明 |
|---|---|---|
| 后端框架 | **Spring Boot 2.7.18 + quectel-code-parent** | Java 8，`javax.*`，端口 8081，context-path `/sales-lead-hub` |
| starter | **web + security + mysql** | ⚠️ security 必带 mysql，否则启动 `NoClassDefFoundError` |
| 持久层 | **MySQL + MyBatis-Plus** | `createBy/updateBy` 由 security-starter `SecurityMetaObjectHandler` 填充（需 mysql-starter 并存）；dev 库 `sales_lead_hub_server`(192.168.10.28) |
| 认证 | **企业 SSO（UAA/OAuth2.0）** | `SecurityUtils.getCurrentUser()`；无本地账密；gateway-url 切环境 |
| 前端框架 | **QMonoX**（Vue3 + `<script setup>` + TS + Vite + pnpm monorepo） | 端口 8080，路径 `/web`，q-cli |
| 包管理 | 后端 Maven（内网 Nexus）／前端 **pnpm**（非 npm/yarn） | |
| 门禁 | 前端 self-check / gate-check / env.lock；后端编码规范 | **黑盒，以 skill + 门禁为准** |
| 施工基准 | `PRD/商机平台/决策纪要与修正基线.md` + `产品文档-v2.0/` | 功能/字段/流转唯一依据 |

**后端包结构（`com.quectel.web.cloud.salesleadhubserver`）：** `controller/` · `service/impl/` · `dao/` · `mapper/` · `pojo/{entity,dto,vo,enums}` · `convert/`
**前端结构（`sales-lead-hub-web/apps/web-app/src/`）：** `pages/`（definePage 路由）· `apis/`（请求契约层）· `components/` · `layouts/` · `hooks/` · `locale/` · `error-pages/`

---

## 角色定义

### 1. Team Lead（主 Agent）
- 职责：整体协调、任务拆分、进度把控、最终验收；不直接写业务代码。
- 约束：跨角色变更最终决策者；等子 Agent 完成后汇总向用户中文汇报；**skill 与用户指令冲突时代表团队先问用户**。

### 2. Product Manager（产品经理）
- 职责：需求澄清、验收标准、范围边界；**以 PRD 决策基线为唯一口径**。
- 约束：任何新需求先 `superpowers:brainstorming`；越出 v1.0 42 FEAT / 6 模块（含 MOD-04 互动）范围（讨论区/情报/工具/会议/我的任务/批量/吐槽墙等 7 项下期）须先确认；SLA（本期仅首响时限）/评论≤2级/北极星等规则以决策基线为准，不擅改。

### 3. Architect（架构师）
- 职责：技术选型、API 契约（Swagger/OpenAPI 为 SSOT）、starter 取舍、MySQL 表模型、前后端联调协议。
- 约束：
  - **API 路径/字段/响应格式变更须经 Architect 审查**：改后端 + 更新 OpenAPI → 审 → 前端据此对齐 `apis/`，审后 broadcast 通知 Frontend + Backend + QA。
  - **context-path 是三处硬耦合**（`application.yml` context-path + 根 `env/.env` base-url + `q-cli.config.ts` proxy key/rewrite），换上下文须三处字面量同步一致、改完联调 `/me` 验证，任一处漏改=全站 404。
  - 守住 **A2 单体决策**：不接入 org/upm-starter、不启用 Nacos 注册/配置（Spring Cloud BOM 由 parent 继承仅作仲裁）；部门树 owner/产品线/SLA 升级负责人走运营本地维护。
  - 引新 starter（oss/es 等）属架构级变更，须 Architect + Team Lead 双确认。
  - **env 与门禁规则是黑盒，任何改动必须能通过 `env-lock-verify` / self-check**。

### 4. Frontend Engineer（前端工程师）
- 工作目录 `sales-lead-hub-web/`；**贯穿使用 `Quectel-code vue-coding` skill，入场先跑 Step 0**。
- 技术规范：
  - 只用 `<script setup>` + Composition API；请求走 `src/apis/*`；文案走 `vue-i18n` 双语。
  - **路由/页面严格按 `page-rules/routes.md` 5 铁律 + demo 模板**；消 404 = 建 `layout:'default'` 业务页，别动 definePage/env。
  - **env 只按白名单改**（仅根 `env/.env` 的 `VITE_API_DEFAULT_SERVICE_BASE_URL`），**改完必重跑 `gate-check.mjs --gate=env-lock-create` 重快照**；SSO 客户端集成始终生效，`VITE_DEV_SSO_ENABLE=N` 属禁改项保持 N，别误当"关 SSO"去改。
  - 写完必自跑 self-check 到 PASS，不把 FAIL 抛给用户。
- 禁止：越权改 env / 改框架内置页（`index.vue`/`login`/`error-pages`）/ 引 `package.json` 外新依赖 / 用 npm·yarn / 项目落中文路径。

### 5. Backend Engineer（后端工程师）
- 工作目录 `sales-lead-hub-server/`；使用 `Quectel-code java-coding` skill，**编码前必读** GitLab Java 编码规范（curl 链接见根 `CLAUDE.md` 第 6 节）。
- 技术规范：
  - 分层 Controller → Service(impl) → Dao/Mapper（当前仓库仅 controller 骨架，按此约定新建）；鉴权只加 `@PreAuthorize`（勿重复 `@EnableGlobalMethodSecurity`；首个受保护接口须验"无权限角色应 403"确认开关生效）。
  - 取用户用 `SecurityUtils.getCurrentUser()`；**不自建本地账密/密码哈希/锁定**（UAA 职责）。
  - **pom 的 security 必带 mysql-starter**；`javax.*` 非 `jakarta.*`；日志 SLF4J，密码/token 不入日志。
  - 敏感口令入 `application-local.yml`（gitignored），`application.yml` 只留占位。
- 禁止：未通知 Architect 改接口路径/字段/响应；引 `pom.xml` 外新依赖；提交明文口令。

### 6. QA Engineer（测试工程师）
- 验收重点：
  - **前端门禁**：self-check（挂 `predev` 自动跑）全 PASS。
  - **SSO 登录**：UAA 网关可达时登录成功；网关不可达返回 401（确认无本地账密降级）。**网关不可达时该项标 blocked、不计 PASS**；本地登录路径与前置条件由 Architect 明确后据以执行。登出判定点（gateway logout / store 清 token 的可观察表现）须先定义。
  - **联调**：`/me`、`/me/id` 通；**验 context-path 三处字面量一致**（`application.yml` + `env/.env` + `q-cli.config.ts`），rewrite 后对得上。
  - **权限**：以 PRD 角色-权限矩阵为验收物，交付附「`@PreAuthorize` 注解清单 ↔ 前端菜单/按钮权限码」对照表逐行核；**首个受保护接口验"无权限角色应 403"**（确认方法级安全真生效，防越权）。
  - **主流程**：按 PRD 决策基线走商机/需求闭环、评论 ≤ 2 级、通知订阅。**SLA 首响时限三级升级**（阈值须可压缩为秒级或提供手动触发入口才可验，不可能真等 2h/4h/24h）。
  - **安全**：Swagger 在 test/prod 已关闭或口令非空；抓一次真实登录/接口日志确认 `Authorization`/token/口令未落日志。
  - i18n 中英切换主流程可用，无硬编码漏译。
- **上线前必过（缺一不可）**：① 前端 self-check + **`pnpm typecheck:all`**（非根 `typecheck`，后者只检 husky）+ `pnpm build:web-app` 全绿；
  ② 后端 `mvn clean package` 打包成功 + **`mvn test` 全绿**（`spring-boot-starter-test` 已就位，`MeControllerTest` 离线契约基线；业务代码须配套 TDD 单元测试）；③ SSO 登录 → 主流程 → 登出全链路联调；④ 安全检查（密钥不落库/日志、无明文口令、Swagger 生产受控）。

---

## 协作硬规则

1. **API 路径 / 字段 / 响应格式变更** → 必须同时通知 @Architect @Backend @Frontend @QA。
2. **架构 / 依赖 / starter / SSO 环境切换** → Architect + Team Lead 双确认。
3. **MySQL 表模型变更** → Architect 审查（注意 MyBatis-Plus 审计字段、索引）。
4. **env / 门禁 / 框架内置页** → 黑盒，任何改动必须过对应门禁；拿不准先问用户。
5. **安全相关改动**（鉴权、密钥、外部 URL、SSO）→ `security-review` + Architect 审查。
6. 完成任务后主动用 `superpowers:requesting-code-review` 请求 Review。

## 强制 Skill 触发规则

| 场景 | 必须调用的 Skill |
|---|---|
| 前端任何开发 | `Quectel-code vue-coding`（入场 Step 0） |
| 后端任何开发 | `Quectel-code java-coding` |
| 新功能 / 需求 / 重构 | `superpowers:brainstorming` |
| 开始实施前 | `superpowers:writing-plans` → EnterPlanMode |
| 遇 bug / 门禁 FAIL | `superpowers:systematic-debugging` |
| 声称完成时 | `superpowers:verification-before-completion` |
| 涉及输入 / 鉴权 / 接口 / 密钥 / SQL | `security-review` |

## 通信规范

| 场景 | 发送方 | 接收方 | 类型 |
|---|---|---|---|
| API 契约变更 | Architect | 所有工程师 | broadcast |
| 接口开发完成 | Backend | Frontend + QA | message |
| 页面开发完成 | Frontend | QA | message |
| 门禁/测试结果 | QA | Team Lead | message |
| 任务汇报 | Team Lead | 用户 | 直接回复（中文） |

- 消息用中文自然语言，含任务状态/完成内容/遇到的问题；禁止发结构化 JSON 状态消息。
- 评审期各评审角色之间可直接 SendMessage 互通、当场对齐分歧；仲裁与最终结论由 Team Lead 汇总。

## 团队启动参考

```
team_name: sales-lead-hub-core
成员: TeamLead / PM / Architect / Frontend / Backend / QA
工作目录: C:\Users\leon.yan\Desktop\dmx\demo-new   （纯 ASCII 路径，QMonoX 门禁要求）
前端: sales-lead-hub-web，:8080/web（start-web.ps1，pnpm dev:web-app）
后端: sales-lead-hub-server，:8081，context-path /sales-lead-hub（start-server.ps1，mvn spring-boot:run）
数据库: MySQL dev 192.168.10.28 库 sales_lead_hub_server（口令走 application-local.yml）
SSO: UAA/OAuth2.0 dev 网关 http://192.168.10.27:8088/api
依赖源: 后端内网 Nexus（NO_PROXY 绕外部代理）；前端 pnpm workspace
施工基准: PRD/商机平台/决策纪要与修正基线.md + 产品文档-v2.0/
```
