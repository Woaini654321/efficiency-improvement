# 项目长期记忆 — 销售商机互助平台（sales-lead-hub）

最后更新：2026-07-18

## 0. 第一原则：Quectel-code 框架是黑盒

- **严格按 skill 执行，不跳步、不自由发挥。** 开源通用知识不足以理解这套框架，以 skill/门禁为准。
- **skill 与用户指令冲突 → 先问用户；不回复则严格按 skill。**
- 前端入场必跑 `vue-coding` skill **Step 0**（项目根）：`permissions-sync → version-check → reference-sync → self-check`；
  reference-sync 把 23 份权威规范拉进 `.quectel-code/cache/reference/`（`env-rules.md`、`page-rules/routes.md` 等）。不 sync 就是瞎猜。
- 写完必自跑门禁到 PASS，不把 FAIL 抛给用户。血泪教训：越权改 env → **白屏 + 全 404**。

## 1. 核心技术栈与约束（永久记住）

- 后端：**Spring Boot 2.7.18 + quectel-code-parent + Java 8**；`javax.*` 非 `jakarta.*`；包根 `com.quectel.web.cloud.salesleadhubserver`。
- starter：**web + security + mysql**。⚠️ security-starter 的 `SecurityMetaObjectHandler` 配置扫描期即引用 MyBatis-Plus `MetaObjectHandler`——**security 必带 mysql-starter**，否则**启动**期 `NoClassDefFoundError`（编译能过，启动才炸）。
- 持久层：**MySQL + MyBatis-Plus**；`createBy/updateBy` 由 **security-starter 的 `SecurityMetaObjectHandler`** 填充（依赖 mysql-starter 的 MyBatis-Plus，故两 starter 必并存——也是"security 必带 mysql"的根因）；dev 库 `sales_lead_hub_server`@192.168.10.28（url 带 `createDatabaseIfNotExist` 自建）。**无业务操作审计中台**（log-starter 仅 APM），需自建审计表 + AOP。
- 认证：**企业 SSO（UAA/OAuth2.0）**，非本地账密。`TokenValidationFilter` 取 `Authorization` 头回调 `{gateway}/uaa/current` 远程校验（本地不验签/不存密码），业务侧零登录代码。**不要自建本地账密库/密码哈希/锁定**（UAA 职责）；UAA 不可达即 401，无「降级本地账密」。gateway-url（填 `quectel.base.gateway-url`，到 `/api` 为止，`/uaa/current` 由 starter 追加）：dev `http://192.168.10.27:8088/api`（实测）/ 测试 `https://fat-softweb.quec.com/api` / 生产 `https://openapi.quectel.com/api`（⚠️ 测试/生产值**待联调验证勿照抄**，生产强制 https）。
- 鉴权：框架**应**已开 `@EnableGlobalMethodSecurity`，业务只加 `@PreAuthorize`（勿重复声明）；取用户 `SecurityUtils.getCurrentUser()` / `getCurrentUserId()`。⚠️ 该开关未在仓库源码证实，首个受保护接口须验"无权限角色应 403"，确认后回填本记忆。
- 前端：**QMonoX**（Vue3 + `<script setup>` + TS + Vite + **pnpm monorepo**，q-cli）；**包管理 pnpm，非 npm/yarn**；SSO 客户端集成由 `main.ts` `.use(store)` 内建、始终生效；`VITE_DEV_SSO_ENABLE=N`（本地 dev SSO 登录模式）属白名单禁改项、保持 N，别误当"关 SSO"。
- 端口：前端 **8080**（`pnpm dev:web-app`，路径 `/web`）；后端 **8081**，context-path `/sales-lead-hub`。**三处硬耦合**：后端 `application.yml` context-path + 根 `env/.env` `VITE_API_DEFAULT_SERVICE_BASE_URL=/sales-lead-hub/` + `q-cli.config.ts` proxy key(`/api/sales-lead-hub`)/rewrite，换上下文三处须同改一致，漏一处=全站 404。出站路径 = `VITE_API_PROXY`(`/api`,禁改) + base + url，rewrite 去 `/api` 后打后端 context-path。
- i18n：`vue-i18n` 双语 + 后端 `MessageSource`，强制中英双语。
- Commit：**中文 Conventional Commits**（跟随仓库历史，如 `feat:对产品需求进行分析`）；husky + commitlint 校验。
- **架构决策 A2：不接入 org/upm-starter、不启用 Nacos 注册/配置，保持单体**（Spring Cloud/Alibaba BOM 由 parent 继承仅作版本仲裁，非接入微服务治理）。UAA 登录态只给 7 字段（id/username/phone/email/name/tenantId/roles，**无部门/工号**）；部门树 owner/产品线/用户归属/SLA 升级负责人全部运营本地维护。
- 按需引 starter：全文搜索 `elasticsearch-starter`+`EsHelper`；文件 `oss-starter`(`ossTemplate` 预签名 URL)+前端 `q-upload`/`q-file-list`（勿自建上传接口）。

## 2. env 白名单铁律（前端硬红线，`env-rules.md`）

- 根 `env/.env`：**只允许改 `VITE_API_DEFAULT_SERVICE_BASE_URL`**（当前 `/sales-lead-hub/`）。
- `apps/*/env/`：**一字不动**；其余所有 `VITE_*`（`VITE_APP_ID`/`VITE_USE_ENV_AS_FRONT_END_APP_CONF`/`VITE_DEV_SSO_*`…）**全禁改**。
- **合法改完 `VITE_API_DEFAULT_SERVICE_BASE_URL` 后必须重快照**：`node <SKILL_DIR>/cli/gate-check.mjs --gate=env-lock-create` 重生成 env.lock（整文件 hash 变了否则 `env-lock-verify` FAIL）；`env-lock-verify` 内嵌 `step-complete` 由 skill 自动跑，不单独调用。
- 越权乱改其他行致白屏/404 时修复 = `git checkout HEAD -- <env文件>` 还原 pristine（仅越权场景用，勿用在合法改动上）。
- AI 防腐层 `VITE_AI_GUARD_API_KEY` 放本地 `env/.env.customize`（已 gitignore），勿提交。

## 3. 路由/页面与门禁坑

- 路由/页面严格按 `page-rules/routes.md` 5 铁律 + demo 模板；`login/`、`error-pages/` 框架内置**禁改**；`index.vue` 是项目源码（非框架内置），本项目管理后台采方案1自动重定向、本项目内禁改（C 端可改方案2）。
- **「404 所有页面」真因 = 缺少带 `layout:'default'` 的业务页**（`genRoutes()` 静默丢弃全部路由），不是 definePage/env 配错。消 404 = 按 `crud.md` 建第一个 `layout:'default'` 业务页。
- **项目路径必须纯 ASCII**：self-check 拿页面绝对路径匹配中文，含中文路径永久 FAIL。
- **移动/重命名前端根目录后** workspace junction 断裂、`apps/*/node_modules` 变空 → 根目录重跑 `pnpm install` 秒级修复。
- 移动含 pnpm node_modules 的项目：用 PowerShell `Move-Item`（bash mv 会因 junction/占用失败），先杀占用的残留 node/esbuild 进程。

## 4. 安全红线

- **不提交任何明文口令。** 后端敏感口令入 `application-local.yml`（同目录 `.gitignore` 已忽略），生产走环境变量 `${DB_PASSWORD}`/`${SWAGGER_PASSWORD}`；`application.yml` 只留占位。
- 密码/token/Authorization 头绝不写入日志。
- **Swagger 生产红线**：test/prod 必须关闭或置网关鉴权后，`SWAGGER_PASSWORD` 生产非空（当前空默认会静默回落 admin/空，fail-open）。生产 DB 用最小权限账号（禁 root 与 `createDatabaseIfNotExist`）、链路强制 TLS。
- `VITE_AI_GUARD_API_KEY` 真 key 必写本地 `env/.env.customize`（需自建、已 gitignore），**切勿写入被跟踪的 `env/.env`**。
- 涉及输入/鉴权/外部接口/密钥/SQL → `security-review`；发现漏洞立即指出并给修复。

## 5. 施工基准与产品口径（PRD v2.0）

- **施工唯一基准** = `PRD/商机平台/决策纪要与修正基线.md`（D1~D9 + §五 TC 台账）+ `PRD/商机平台/产品文档-v2.0/`。
- v1.0 只做核心闭环 **42 FEAT**（讨论区/情报中心/工具助手/会议任务/我的任务/批量发布/吐槽墙 7 项移下期）。
- 模块（共 6 个，9+12+7+3+7+4=42）：`MOD-01 商机信息管理`、`MOD-02 需求与方案匹配`、`MOD-03 通知与订阅`、`MOD-04 互动与反馈`（评论≤2级/收藏/点赞，嵌入 MOD-01/02）、`MOD-05 运营管理后台`（含公告 PC-24/25 + 产品线维护 PC-26=SLA 升级人映射源）、`MOD-06 系统支撑`。
- 规则：**SLA 自动计时的是首响时限**——critical 2h/urgent 4h/normal 24h（起点=created_at）；固定三级升级 L1→L2→L3、**每级间隔=该等级首响时限**，收到首个方案即停表。**本期仅监控首响时限**，解决时限/deadline 属下期（需求不建 deadline，超期关闭=待响应默认 7 天自动关）。评论 ≤ 2 级；北极星 = 需求响应闭环率。

## 6. 常用路径速查

**后端（`sales-lead-hub-server/`）** — `src/main/java/com/quectel/web/cloud/salesleadhubserver/`
- `controller/`（示例 `MeController` → `/me`、`/me/id`）· `service/impl/` · `dao/` · `mapper/` · `pojo/{entity,dto,vo,enums}` · `convert/`
- 配置：`src/main/resources/application.yml`（占位 + gateway-url）；口令 `application-local.yml`（gitignored，例见 `.example`）；i18n `resources/i18n/`
- `pom.xml`（parent=quectel-code-parent）；`start-server.ps1`。（GitLab Java 编码规范 curl 链接见根 `CLAUDE.md` 第 6 节）

**前端（`sales-lead-hub-web/`，QMonoX monorepo）** — 业务在 `apps/web-app/src/`
- `pages/`（definePage 路由：`index.vue` 方案1、`login/`、业务页）· `apis/`（请求契约层）· `components/` · `layouts/` · `hooks/` · `locale/` · `error-pages/`
- `main.ts`（`.use(store)` 内置 SSO）· `q-cli.config.ts`（代理 rewrite）· `env/.env`（子包，禁改）
- 根 `env/.env`（仅 `VITE_API_DEFAULT_SERVICE_BASE_URL` 可改）· `.quectel-code/`（门禁/cache/reference）· `start-web.ps1`

## 7. 构建与运行速查

- 后端起：`start-server.ps1`（`mvn spring-boot:run`，`-Package` 打 jar；预检 Nexus/MySQL/UAA）→ :8081；打包 `mvn clean package`；依赖走内网 Nexus（`NO_PROXY` 需自行预配，脚本不代设）。**测试 `mvn test` 是真门禁**：`spring-boot-starter-test`(test scope) 已加、`MeControllerTest` 为离线契约基线（纯 JUnit+反射，不加 `@SpringBootTest` 以免硬依赖 MySQL/UAA 假红）；业务代码须按 java-coding TDD 配套 service 单元测试，集成测试用 `@SpringBootTest`+Testcontainers（依赖可达时）。
- 前端起：`start-web.ps1`（`pnpm dev:web-app`，`q-cli-run` 缺失自动 install 自愈）→ :8080/web；类型 **`pnpm typecheck:all`**（根 `pnpm typecheck` 只检 husky、业务代码假绿）；构建 `pnpm build:web-app`。
- 门禁：前端 self-check（挂 `predev` 自动跑）+ skill 内部 `gate-check.mjs` 必 PASS 再交付。

## 8. 最后提醒

- 所有 Write/Edit 需用户手动确认权限；git 写操作（commit/push）由用户自己执行，Claude 只允许 `status/diff/log/show`。
- 启动脚本存 **UTF-8 带 BOM**（PowerShell 中文兼容）。
- 相关记忆：`quectel-code-suite-setup`（套件安装/内网 git）、`feedback-quectel-vue-skill-strict`（黑盒纪律元规则）、`pnpm-windows-rename-relink`。
