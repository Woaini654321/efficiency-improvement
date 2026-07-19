# CLAUDE.md — 销售商机互助平台（sales-lead-hub）

最后更新：2026-07-18

> 本文件是本仓库对 Claude Code 的**强制规则**，优先级高于默认行为与全局个人偏好。
> 本项目基于 **Quectel-code 企业内部框架**（前端 QMonoX / 后端 quectel-code-parent），
> 框架把大量约定固化进**门禁**（self-check / gate-check / env.lock）与**权威参考规范**。
> **框架是黑盒：以 skill 与门禁为准，禁止凭开源通用经验自由推断。** 这是本项目的第一原则。

---

## 0. 黑盒纪律（本项目最高红线，务必先读）

- **严格按 skill 执行，不跳步、不自由发挥。** 你的开源通用知识不足以理解这套框架；遇事以 `Quectel-code vue-coding` / `java-coding` skill 和门禁为准。
- **skill 与用户指令冲突 → 先用 AskUserQuestion 问用户；用户不回复则严格按 skill。**（用户显式定的元规则）
- **前端入场必跑 vue-coding skill Step 0**（在 `sales-lead-hub-web` 根执行）：
  `permissions-sync → version-check → reference-sync → self-check`。
  reference-sync 会把 23 份权威规范拉进 `.quectel-code/cache/reference/`（含 `env-rules.md`、`page-rules/routes.md`）。
  **不 sync 就没有权威文档，只能瞎猜——这正是要消灭的行为。**
- **写完必自跑门禁到 PASS**（self-check / gate-check），不把 FAIL 抛给用户（自修复铁律）。
- 违反黑盒纪律的真实代价（已发生）：越权改 env → **应用白屏 + 全站 404**。

## 1. 回复与沟通

- 所有回复用中文，专业、简洁、高效；不用表情符号（除非我明确要求）。
- 需求不明确先用 AskUserQuestion 澄清，不猜测。
- 改文件前先说明计划、等确认；每次改动最小范围；改完简述改了什么、为什么。
- 权衡性问题优先用文字讨论利弊 + 给推荐，而非连续多个选择题。

## 2. 项目概况

销售商机互助平台：销售之间发布/认领商机与需求、方案匹配、通知订阅、运营后台。
- **施工唯一基准 = PRD 决策基线**：`PRD/商机平台/决策纪要与修正基线.md`（含 D1~D9 决策 + §五 TC 台账）
  与 `PRD/商机平台/产品文档-v2.0/`。功能范围、状态流转、字段以此为准，不凭原型或本文件臆断。
- v1.0 只做核心闭环 **42 FEAT**（讨论区/情报中心/工具助手/会议任务/我的任务/批量发布/吐槽墙移下期）。
- 模块（共 6 个，FEAT 数 9+12+7+3+7+4=42）：`MOD-01 商机信息管理`、`MOD-02 商机需求与方案匹配`、
  `MOD-03 通知与订阅`、`MOD-04 互动与反馈`（评论 ≤ 2 级/收藏/点赞，嵌入 MOD-01/02 页面）、
  `MOD-05 运营管理后台`（含公告发布 PC-24/25 + 产品线维护 PC-26，PC-26 是 SLA 升级人映射的数据来源）、`MOD-06 系统支撑`。
- 关键规则：**SLA 自动计时的是首响时限**——特急 critical 2h / 紧急 urgent 4h / 普通 normal 24h（计时起点=需求 created_at）；
  固定三级升级 L1→L2→L3，**每级间隔=该等级首响时限**，收到首个方案即停表停升级。**本期仅监控首响时限**；
  解决时限/deadline 属下期（需求不建 deadline，超期关闭=待响应默认 7 天系统自动关闭）。评论 ≤ 2 级；北极星 = 需求响应闭环率。
- 目录：后端 `sales-lead-hub-server/`（规范并入本文件 §6）、前端 `sales-lead-hub-web/`；
  长期记忆见 `MEMORY.md`，多角色协作见 `AGENTS.md`。

## 3. 技术栈基准（务必记准，勿套用 MongoDB / 自建 JWT 项目）

| 层 | 技术 | 说明 |
|---|---|---|
| 后端框架 | **Spring Boot 2.7.18 + quectel-code-parent** | `javax.*` 而非 `jakarta.*` |
| JDK | **Java 8（1.8）** | 无 records/sealed；高版本语法糖先确认 |
| starter | **web + security + mysql** | ⚠️ security 必须同时带 mysql，否则**启动**期 `NoClassDefFoundError`（编译能过） |
| 持久层 | **MySQL + MyBatis-Plus**（quectel-code-mysql-starter） | 关系型；`createBy/updateBy` 由 **security-starter 的 `SecurityMetaObjectHandler`** 填充（依赖 mysql-starter 提供的 MyBatis-Plus，故两 starter 必并存） |
| 认证 | **企业 SSO（UAA / OAuth2.0）** | 非本地账密；`SecurityUtils.getCurrentUser()`；`gateway-url` 切环境 |
| 鉴权 | 框架**应**已开 `@EnableGlobalMethodSecurity` | 业务只加 `@PreAuthorize`，禁止重复声明；⚠️ 该开关未在本仓库源码证实，**首个受保护接口须验"无权限角色应 403"** |
| 包根 | `com.quectel.web.cloud.salesleadhubserver` | 分层**约定** controller/service/impl/dao/mapper/pojo/convert（当前仓库仅有 controller 骨架） |
| 构建 | Maven（依赖走内网 Nexus） | 端口 **8081**，context-path `/sales-lead-hub` |
| 前端框架 | **QMonoX**（Vue3 + `<script setup>` + TS + Vite + pnpm monorepo） | 禁 Options API / class 组件 |
| 包管理（前端） | **pnpm**（workspace） | **不是 npm、不是 yarn** |
| 路由/页面 | q-cli `definePage` + `layout` | 严格按 `page-rules/routes.md` 5 铁律 + demo 模板 |
| SSO（前端） | `main.ts` 的 `.use(store)` 天然内置 | SSO 客户端集成始终生效；`VITE_DEV_SSO_ENABLE=N` 是本地 dev SSO 登录模式、属白名单禁改项保持 N，**别误当"把 SSO 关了"去改** |
| i18n | `vue-i18n` 双语 + 后端 `MessageSource` | 强制中英双语 |
| 端口 | 前端 **8080**（`pnpm dev:web-app`，路径 `/web`）；后端 **8081** | 错开避免本机同起冲突 |

**架构决策（PRD 二次决策 A2）：不接入 org-starter/upm-starter，不启用 Nacos 注册/配置中心，保持单体。**
（Spring Cloud / Alibaba BOM 由 quectel-code-parent 继承、仅作版本仲裁，不代表接入微服务治理。）
UAA 登录态只给 7 字段（id/username/phone/email/name/tenantId/roles，**无部门/工号**）；
部门/产品线/用户归属/SLA 升级负责人（含部门树 owner）**全部运营本地维护**，不依赖 `org-starter` 的 `getUserSupervisorIds`。

**API 契约治理：** 后端 Swagger/OpenAPI 为契约 SSOT；接口路径/字段/响应变更 → 改后端 + 更新 OpenAPI → Architect 审 → 前端据此对齐 `src/apis/*`（见 `AGENTS.md`）。**换 context-path 须同步改三处字面量**（见 §4）。

**按需引 starter（勿自建轮子）：** 全文搜索 `elasticsearch-starter`；文件存储 `oss-starter`（`ossTemplate` 预签名 URL + 前端 `q-upload`/`q-file-list`，勿自建上传接口）；业务操作审计需自建审计表 + AOP（`log-starter` 仅链路 APM，非业务审计）。

## 4. env 白名单（前端硬红线，`env-rules.md` 铁律）

- 根 `env/.env`：**只允许改 `VITE_API_DEFAULT_SERVICE_BASE_URL` 这一个变量**（当前 `/sales-lead-hub/`）。
- `apps/*/env/`（如 `apps/web-app/env/.env`）：**一字不能动**。
- 其余所有 `VITE_*`（`VITE_APP_ID` / `VITE_USE_ENV_AS_FRONT_END_APP_CONF` / `VITE_DEV_SSO_*` / `VITE_API_PROXY` …）：**全部禁改**。
- **合法改完 `VITE_API_DEFAULT_SERVICE_BASE_URL` 后必须重快照**：跑 `node <SKILL_DIR>/cli/gate-check.mjs --gate=env-lock-create` 重生成 env.lock，否则整文件 hash 变了会 `env-lock-verify` FAIL。（`env-lock-verify` 内嵌在 `step-complete` 门禁里，由 skill 自动跑，不单独调用。）
- 越权乱改其他行致白屏/404 时，修复 = `git checkout HEAD -- <env文件>` 还原 pristine（env 被 git 跟踪）；此还原**仅用于越权场景**，勿用在上面的合法改动上（会把改动整个撤销）。

- **⚠️ context-path 是三处硬耦合，不是"env 一处成对"**：换服务上下文须**同步改三处字面量并保持一致**——
  ① 后端 `application.yml` 的 `server.servlet.context-path`；
  ② 前端根 `env/.env` 的 `VITE_API_DEFAULT_SERVICE_BASE_URL`；
  ③ 前端 `apps/web-app/q-cli.config.ts` 的 proxy key（`/api/sales-lead-hub`）+ rewrite 正则（`/^\/api\/sales-lead-hub/`）。
  `q-cli.config.ts` 不受 env-lock 保护但与 context-path 强耦合，漏改任一处 → 请求打不进代理 → **全站 404**。改完必 self-check + 联调 `/me` 验证。
  （前端出站路径 = `VITE_API_PROXY`(`/api`，框架固定禁改) + base(`/sales-lead-hub/`) + url；vite 代理命中后 rewrite 去掉 `/api` → 后端 context-path。）

## 5. 路由与页面（前端）

- 严格按 `page-rules/routes.md` 5 铁律 + demo 模板照抄，别自由发挥字段位置 / layout 值。
- `login/`、`error-pages/` 是框架内置，**禁改**（routes.md 铁律）。`index.vue` 是项目重定向入口（非框架内置），本项目为管理后台采**方案1自动重定向、本项目内禁改**（C 端场景 routes.md 允许改方案2）。
- **「404 所有页面」的真因通常不是 definePage 配错，而是缺少带 `layout:'default'` 的业务页**：
  没有顶层 `layout:'default'` 页 → `genRoutes()` 静默丢弃全部路由 → 全 404。
  消 404 = 按 `crud.md`「新建模块」建第一个 `layout:'default'` 业务页，不是去动 definePage/env。
- **项目路径必须纯 ASCII**：QMonoX self-check 门禁拿页面绝对路径匹配中文，含中文路径会永久 FAIL。

## 6. 代码风格

**前端（QMonoX / Vue3 + TS）**
- 只用 `<script setup>` + Composition API；严格模式 TS。
- 组件 PascalCase，变量/函数 camelCase，常量 UPPER_CASE；ES Module，禁 CommonJS。
- 展示文案走 `vue-i18n`，禁硬编码；请求统一走 `src/apis/*`，勿在组件散写 fetch。
- 组件卸载清理定时器/订阅/AbortController。

**后端（Java 8 + Spring Boot 2.7 + quectel-code）**
- 分层 Controller → Service(impl) → Dao/Mapper；标准 Java 命名。
- 鉴权只加 `@PreAuthorize`（框架**应**已开全局方法安全，勿重复 `@EnableGlobalMethodSecurity`；首个受保护接口须验"无权限角色应 403"，确认开关生效）。
- 取当前用户用 `SecurityUtils.getCurrentUser()` / `getCurrentUserId()`，不自造上下文。
- 日志用 SLF4J，禁 `System.out.println`；**密码 / token / Authorization 头绝不写入日志**。
- 注意 `javax.*`（非 `jakarta.*`）。**编码前必读 Quectel Java 编码规范**（内网 GitLab，需 token）：
  `curl -s -H "PRIVATE-TOKEN: $GITLAB_TOKEN" "https://git.quectel.com/api/v4/projects/1491/repository/files/quectel-code-java%E5%BC%80%E5%8F%91%E6%96%87%E6%A1%A3%2F%E7%BC%96%E7%A8%8B%E8%A7%84%E8%8C%83.md/raw"`

## 7. 安全与凭据

- **不提交任何明文口令。** 后端敏感口令放 `application-local.yml`（同目录 `.gitignore` 已忽略，不提交），
  生产走环境变量 `${DB_PASSWORD}` / `${SWAGGER_PASSWORD}`；`application.yml` 只留占位。
- **认证是企业 SSO（UAA/OAuth2.0），不是本地账密**：`TokenValidationFilter` 取 `Authorization` 头回调
  `{gateway}/uaa/current` 远程校验（本地不验签/不存密码），业务侧零登录代码。
  **故不要自建本地账密库 / 密码哈希 / 锁定**（那是 UAA 职责）；UAA 不可达即 401，不存在「降级本地账密」。
  gateway-url（填入 `quectel.base.gateway-url`，到 `/api` 为止，`/uaa/current` 由 security-starter 追加）：
  dev = `http://192.168.10.27:8088/api`（实测值）；测试/生产 = `https://fat-softweb.quec.com/api` / `https://openapi.quectel.com/api`
  （⚠️ 测试/生产值**待联调验证，勿直接照抄**；生产 UAA 链路强制 https）。
- **Swagger 生产红线**：API 文档在 test/prod **必须关闭或置于网关鉴权后**，`SWAGGER_PASSWORD` 生产必须非空（当前 `application.yml` 空默认会静默回落 admin/空口令，属 fail-open）。
- 前端 AI 防腐层 `VITE_AI_GUARD_API_KEY` 放本地 `env/.env.customize`（已 gitignore，文件需自建），**切勿写入被 git 跟踪的 `env/.env`**。
- 生产 DB 用专用最小权限账号（禁用 root 与 `createDatabaseIfNotExist`）、链路强制 TLS。
- 涉及用户输入 / 鉴权 / 外部接口 / 密钥 / SQL → 主动 `security-review`；发现漏洞立即指出并给修复。

## 8. 工作流程（强制）

- 本项目使用 Superpowers 插件，纪律优先于直觉。
- 新需求/功能/重构 → **先 `superpowers:brainstorming`**（前端另叠 `Quectel-code vue-coding` skill）。
- 多文件变更/架构调整 → `superpowers:writing-plans` → **EnterPlanMode**，输出步骤 + 风险点，等我确认。
- 改前必先 Read，优先 Edit 精准替换，不整文件 Write。
- 声称「完成」前 → `superpowers:verification-before-completion`（门禁 + 构建 + 联调）。
- 标准链路：`需求 → brainstorming → writing-plans → 实施 → 门禁/验证 → 完成`。

## 9. 构建与运行命令

> 只在需要验证时运行读/构建/门禁命令；**禁止**安装/部署/git 写等高危命令（见第 11 节）。

- 后端起服务：`sales-lead-hub-server/start-server.ps1`（默认 `mvn spring-boot:run`，`-Package` 打 jar；
  启动前预检 Nexus/MySQL/UAA）；起在 **:8081**，context-path `/sales-lead-hub`。
- 后端编译/打包：`mvn compile` / `mvn clean package`（依赖走内网 Nexus；`NO_PROXY` 需在 shell/Maven settings 预先配好，脚本不代设；预检仅 TCP 可达、不代表 mvn 能穿代理下载）。
- 后端测试：`mvn test`——测试工具链（`spring-boot-starter-test`）已就位、`MeControllerTest` 为离线契约测试基线；**是真门禁，必须全绿**。新增业务代码须按 java-coding TDD 配套 service 层单元测试；跑全上下文/DAO 的集成测试用 `@SpringBootTest` + Testcontainers（仅依赖可达时），**勿把硬依赖 MySQL/UAA 的测试塞进离线门禁**。
- 前端起服务：`sales-lead-hub-web/start-web.ps1`（默认 `pnpm dev:web-app`，检测 `q-cli-run` 缺失自动 install 自愈）；
  起在 **:8080**，路径 `/web`。
- 前端类型检查：**`pnpm typecheck:all`**（覆盖业务源码；根 `pnpm typecheck` 只检 `.husky`、业务代码假绿，勿用）；构建 `pnpm build:web-app`。
- 前端门禁：由 `Quectel-code vue-coding` skill 驱动（内部 `node <SKILL_DIR>/cli/gate-check.mjs`）；self-check 已挂 `web-app` 的 `predev` 钩子、`pnpm dev:web-app` 前自动跑。
- **移动/重命名前端根目录后**：workspace junction 断裂、`apps/*/node_modules` 变空 → 根目录重跑 `pnpm install` 秒级修复。

## 10. Git 操作策略（严格限制）

- **禁止** Claude 自动执行 `git commit`/`push`/`rebase`/`reset` 等写操作；允许 `status`/`diff`/`log`/`show`。
- 变更就绪时用中文提醒，例如：「文件已改完，可手动 `git add . && git commit -m '…'`」。
- **Commit 规范：中文 Conventional Commits**（跟随本仓库历史，如 `feat:对产品需求进行分析`）；
  husky + commitlint（config-conventional）会校验，`type:` 前缀必须合法（feat/fix/refactor/docs/test/chore…）。

## 11. 永远禁止（红线）

- 执行 `pnpm/npm/yarn install`（自愈脚本除外）、`mvn install`/`deploy`、`git push`、`git reset --hard`、`rm -rf` 等高危命令。
- 引入 `package.json` / `pom.xml` 之外的新依赖（须我明确同意）。（已批准：后端 `spring-boot-starter-test`，test scope。）
- 越权改 env（见第 4 节）、改框架内置页（`index.vue`/`login`/`error-pages`）、改 env.lock/门禁规则。
- 提交明文口令、把项目放到含中文的路径。
- 后端只带 security-starter 不带 mysql-starter（启动必炸）。

## 12. 必须使用的 Skills

| 场景 | Skill |
|---|---|
| 前端任何开发（入场即用，贯穿全程） | `Quectel-code vue-coding`（黑盒纪律，Step 0 必跑） |
| 后端开发 | `Quectel-code java-coding` |
| 新功能/需求变更/技术选型/重构前 | `superpowers:brainstorming` |
| 进入实施前 | `superpowers:writing-plans` → EnterPlanMode |
| 遇 bug / 测试失败 / 门禁 FAIL | `superpowers:systematic-debugging` |
| 声称「完成」前 | `superpowers:verification-before-completion` |
| 涉及输入 / 鉴权 / 接口 / 密钥 / SQL | `security-review` |

## 13. Memory 习惯

- 架构决策、反复出现的偏好、框架黑盒坑 → 写入 `MEMORY.md` 与个人记忆库。
- 临时任务细节不写入长期记忆。重要结论后可建议：「是否记录到 MEMORY.md？」
