# demo-new 全栈项目框架 · 设计文档

- **日期**：2026-07-17
- **目标**：从 0 搭建前后端全栈项目框架，具备 SSO 登录、附件上传两项基础能力
- **依据**：`C:\Users\leon.yan\Documents\coding-reference` 知识库（`init-backend.md` / `项目骨架模板.md` / `编程规范.md` / `sso-integration.md` / `oss-starter.md` / `create-project.md` / `api-rules/` / `page-rules/` / `coding-standard.md`）

---

## 1. 目标与非目标

### 目标
1. 一个能编译、能启动的 Java 单体后端，接入公司 UAA 单点登录。
2. 一个能 `pnpm dev` 起来的 Vue 前端，通过 q-cli 官方脚手架生成。
3. 通用附件能力：上传到 MinIO、元数据落库、分页查询、预签名 URL 下载、逻辑删除。
4. 前后端联调打通（context-path 与前端 base URL 对齐）。
5. 后续业务开发照抄即可的分层范例（Controller / Service / Dao / Convert / DO / DTO / VO 全套）。

### 非目标（明确不做）
- 不做具体业务模块。附件模块本身即示范。
- 不做 Redis / Kafka / ES。当前两项能力用不到，YAGNI。
- 不做微服务化（不引 cloud-starter、不接 Nacos）。后续需要时加一个 starter + 改配置即可。
- 不改前端框架内置的 `pages/login/`、`pages/error-pages/`（文档明令禁止）。

---

## 2. 关键决策记录

| # | 决策 | 选择 | 理由 |
|---|------|------|------|
| D1 | 架构形态 | **单体**，不引 cloud-starter | 框架 demo 以「能跑起来、能验证」为先；转微服务成本低 |
| D2 | 后端组件 | web + security + oss + mysql | security=SSO，oss=附件存储，mysql=附件元数据 |
| D3 | 附件下载 | **预签名 URL** | 后端不扛文件流，大文件不卡；有效期 30 分钟 |
| D4 | 业务关联字段 | **现在就加** `biz_type` + `biz_id` | 通用附件模块标配，成本≈0，避免后续改表 |
| D5 | 项目命名 | 后端 `demo-new-service`，前端 `demo-new-web` | 对齐公司 `user-service` 命名惯例；目录名 == artifactId，不偏离 `init-backend.md` 派生规则 |
| D6 | 门禁脚本 | **跳过** | `install-gate.mjs` / `clean-demo.mjs` / `gate-check.mjs` 本地不存在（属飞书 skill 包），非功能性阻塞 |

### D5 派生值

| 项 | 值 |
|---|---|
| artifactId | `demo-new-service` |
| groupId | `com.quectel.web.cloud` |
| packageName | `com.quectel.web.cloud.demonewservice` |
| database | `demo_new_service` |
| context-path | `/demo-new-service` |
| 前端 `VITE_API_DEFAULT_SERVICE_BASE_URL` | `/demo-new-service/` |

> **D5 的硬约束**：`context-path` 与前端 `VITE_API_DEFAULT_SERVICE_BASE_URL` 的**首段必须严格一致**，否则所有请求 404（`env-rules.md`）。这两个值改一个必须同步改另一个。

---

## 3. 目录结构

```
demo-new/
├── docs/superpowers/specs/          # 本设计文档
├── demo-new-service/                # 后端
│   ├── pom.xml
│   ├── Dockerfile / .gitignore / .dockerignore / CLAUDE.md
│   └── src/main/
│       ├── java/com/quectel/web/cloud/demonewservice/
│       │   ├── DemoNewServiceApplication.java
│       │   ├── controller/AttachmentController.java
│       │   ├── service/AttachmentService.java
│       │   ├── service/impl/AttachmentServiceImpl.java
│       │   ├── dao/AttachmentDao.java
│       │   ├── mapper/AttachmentMapper.java
│       │   ├── convert/AttachmentConvert.java
│       │   ├── pojo/entity/AttachmentDO.java
│       │   ├── pojo/dto/{AttachmentUploadDTO, AttachmentPageDTO}.java
│       │   ├── pojo/vo/AttachmentVO.java
│       │   └── pojo/enums/AttachmentErrorCode.java
│       └── resources/
│           ├── application.yml
│           ├── db/migration/V1__init.sql
│           └── i18n/{messages.properties, messages_zh_CN.properties}
└── demo-new-web/                    # 前端（q-cli 生成，勿手工拼装）
    ├── env/.env                     # 只改 VITE_API_DEFAULT_SERVICE_BASE_URL
    └── apps/web-app/
        ├── q-cli.config.ts          # dev 代理 → localhost:8080
        └── src/
            ├── apis/attachment/{types.ts, mocks/attachment.json, attachmentAdapter.ts, attachmentApi.ts}
            ├── apis/guard-setup.ts
            ├── components/{q-upload, q-file-list, q-big-table, q-form, q-remote-select}/
            └── pages/attachment/index.vue
```

---

## 4. 后端设计

### 4.1 技术底座
Java 8（仅 `javax.*`，禁 `jakarta.*`）/ Spring Boot 2.7.18 / MyBatis-Plus 3.5.16 / MapStruct 1.5.5。
Parent 为 `com.quectel.code:quectel-code-parent:1.0.0-SNAPSHOT`——**禁止**改用 `spring-boot-starter-parent`（会丢 spring-cloud 版本仲裁）。
所有 `com.quectel.code` 依赖**不写 version**（BOM 仲裁）。**禁止**声明 `maven-compiler-plugin`（会覆盖 parent 的 `annotationProcessorPaths`，导致 MapStruct 生成空映射）。

### 4.2 SSO
引 `quectel-code-security-starter`，配置：
```yaml
quectel:
  base:
    gateway-url: http://192.168.10.27:8088/api    # UAA 地址由此拼装 + /uaa/current
```
取用户：`SecurityUtils.getCurrentUser()` / `getCurrentUserId()`（`com.quectel.code.security.utils`）。
鉴权：Controller 方法直接 `@PreAuthorize`。启动类**禁止** `@EnableGlobalMethodSecurity`（框架 `SecurityConfig` 已开，重复声明冲突）。

**已知风险**：前端 SSO 对接在知识库中**无文档**。登录页 `pages/login/` 是框架内置且禁止修改，token 通过 `useTokenStore().getToken()` 取，但「token 如何被写入」无记载。开发期按 `prototype-defaults.md` 默认策略：不加路由守卫、页面走 whiteList / 已有 mock token。真实 SSO 跳转链路需在联调阶段实测确认。

### 4.3 附件模块

**表 `sys_attachment`**（DDL 细节以 `规范文档/Mysql使用规范.md` 为准）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，`@TableId(type = IdType.ASSIGN_ID)`，**禁 AUTO** |
| file_name | VARCHAR(255) | 原始文件名 |
| object_name | VARCHAR(512) | OSS 对象名，唯一 |
| file_size | BIGINT | 字节 |
| content_type | VARCHAR(128) | MIME |
| biz_type | VARCHAR(64) | 业务类型（可空） |
| biz_id | VARCHAR(64) | 业务单据 ID（可空） |
| status | VARCHAR(16) | **String 类型**，`Y`/`N`（规范禁用 Integer/Boolean 做状态） |
| create_time / update_time / create_by / update_by | — | 审计字段，`SecurityMetaObjectHandler` 自动填，**禁止手动赋值**；字段名硬编码，禁用 `createdAt`/`gmtCreate` |
| deleted | CHAR(1) | 逻辑删除；查询**禁止**手加 `deleted='N'` |

索引：`idx_biz (biz_type, biz_id)`。

**OSS**
```yaml
quectel:
  oss:
    endpoint: https://oss-dev.quectel.com
    access-key: minio
    secret-key: Quectel@2022
    bucket-name: quectel-cli
```
objectName 规则：`demo-new/{yyyyMM}/{uuid}/{originalFilename}`。
multipart 100MB 由 oss-starter 默认提供，**不手写**。

**接口**（全部返回 `Result<T>`）

| 方法 | 路径 | 入参 | 出参 | 说明 |
|------|------|------|------|------|
| POST | `/attachment/upload` | `MultipartFile` + biz_type? + biz_id? | `Result<AttachmentVO>` | 传 OSS + 落库 |
| POST | `/attachment/page` | `AttachmentPageDTO` | `Result<PageVO<AttachmentVO>>` | **分页必须 POST + @RequestBody，禁 GET** |
| GET | `/attachment/presign/{id}` | id | `Result<String>` | 预签名 URL，30 分钟 |
| DELETE | `/attachment/delete/{id}` | id | `Result<Void>` | 逻辑删除（`removeById`） |

**错误码**：`AttachmentErrorCode implements IErrorCode`，从 **10000** 起（10001 文件为空 / 10002 附件不存在 / 10003 上传失败）。
注意 `IErrorCode` 的方法是 `getMessageKey()`，**不是** `getMessage()`；`BaseException` **没有** `(String)` 构造器。

### 4.4 分层与红线
调用链严格 `Controller → Service → Dao → Mapper`。Controller 禁碰 Dao/Mapper；Service 禁 `extends ServiceImpl`（只有 Dao 层可以）。
依赖注入一律 `@Resource`（**禁 `@Autowired`**）。日志只用 `@Slf4j` + `{}` 占位符。
DO↔VO/DTO 转换一律 MapStruct，**禁手写 set**。`@RequestBody` **禁用 Map**。
启动类禁 `@MapperScan`（框架已扫 `com.quectel.**.mapper`）；禁自定义 `MyBatisPlusConfig` 类名；禁实现 `MetaObjectHandler`。
Swagger 注解只用 `io.swagger.v3.oas.annotations.*`。

---

## 5. 前端设计

### 5.1 生成方式
**必须** q-cli 脚手架，手工拼装目录是明令禁止的：
```bash
q-cli create demo-new-web --for AI-First     # --for AI-First 不可省，否则卡交互确认
cd demo-new-web
q-cli add web-app --for AI-First
pnpm install
```
Q 组件（`q-upload` / `q-file-list` / `q-big-table` / `q-form` / `q-remote-select`）**不在 npm**，从 GitLab sparse-clone 后拷入 `apps/web-app/src/components`。克隆失败则**停止，禁止手写组件**。
附加依赖：`pnpm add @ant-design/icons-vue @q-tools/core`（q-upload / q-file-list 必需，缺则 `Failed to resolve import`）。
`main.ts` 注册顺序：`.use(Antd)` **必须在** `.use(QUI)` 之前，否则分页渲染不出来。

### 5.2 API 防腐层
`后端 DTO(snake_case) → adapter → ViewModel(camelCase) → 页面`。页面永不做数据转换。

**头号坑**：`@q-cli/libs` 拦截器在 `code === 0` 时**已自动解包**，adapter 收到的 `raw` 就是内层 payload。写 `raw.data.xxx` → 二次解包 → 页面静默空白。

**AIRequestGuard 只包查询**（分页 / 详情）。上传、增删改**直接调 `request`**，不包 Guard——包了会在 mock 模式下变成 no-op，列表永不刷新。

```typescript
// attachmentApi.ts
export const getAttachmentList = async (params: AttachmentPageParams): Promise<AttachmentPageResult> =>
  (await AIRequestGuard({
    adapter: getAttachmentListAdapter,
    request: () => request.POST<AttachmentPageResult>({ url: 'attachment/page' }, params),
  })) as AttachmentPageResult

export const uploadAttachment = async (formData: FormData): Promise<void> => {
  await request.POST({ url: 'attachment/upload', file: true }, formData)   // 无 Guard
}
```
类型：**所有后端 ID 转 `string`**（int64 > 2^53 会精度丢失）；分页用 `PaginationParams` / `PaginationResult`（`@q-mono-x/types/base`）。
Mock：`mocks/attachment.json`，DTO/snake_case，**≥12 条**（保证第二页可测）。
**联调顺序不可逆**：先切 `mode: 'real'` 验证通过，**再**删 mock。反了会得到「零报错的空白页」——文档称为最难查的失败。

### 5.3 页面
`pages/attachment/index.vue`：列表用 **QBigTable**（禁 `<a-table>`），上传用 **QUpload**（禁 `<a-upload>`），附件展示用 **QFileList**。
QBigTable 列是 vxe 语法（`field`/`title`/`slots`），**不是** antdv 的 `dataIndex`/`customRender`；`height="100%"`，禁 `auto`。
QUpload 的 prop 是 `manual: boolean`——**没有 `mode` prop**（`mode: 'auto'` 是已记录的幻觉写法）。
搜索栏只认 `field`/`label`/`component`/`componentProps` 四个字段，其余静默丢弃。

### 5.4 dev 代理
`apps/web-app/q-cli.config.ts`——**直接 mutate，不 return**（return 会触发 mergeConfig，proxy 合并不可靠）；自定义规则**必须排在** `...existingProxy` **之前**（http-proxy 按 key 顺序首个命中）：
```typescript
export default function (finalConfig: any) {
  const existingProxy = finalConfig.server?.proxy || {}
  finalConfig.server = finalConfig.server || {}
  finalConfig.server.proxy = {
    '/api/demo-new-service': {
      target: 'http://localhost:8080',
      changeOrigin: true,
      rewrite: (path: string) => path.replace(/^\/api\/demo-new-service/, '/demo-new-service'),
    },
    ...existingProxy,
  }
}
```

### 5.5 环境变量红线
根 `env/.env` 里**只有** `VITE_API_DEFAULT_SERVICE_BASE_URL` 可改，其余 `VITE_*` 一律不动，`apps/web-app/env/` 一个字节都不能改。
（`VITE_API_URL` 改了全站 404；`VITE_APP_ID` 改了权限系统 500；`VITE_USE_ENV_AS_FRONT_END_APP_CONF` 改了白屏。）

---

## 6. 环境前置

| 项 | 现状 | 处置 |
|---|---|---|
| Java 1.8.0_492 | ✅ | — |
| Maven 3.9.9 | ✅ | — |
| Node v24.16.0 | ✅ | — |
| pnpm 11.12.0 | ❌ 要求 `>=10 <11` | `npm i -g pnpm@10` |
| q-cli | ❌ 未安装 | `pnpm add -g q-cli --registry=http://192.168.10.107:8081/repository/npm-public/` |
| `~/.m2/settings.xml` | ❌ 无私服配置 | ✅ **已完成**：写入 Nexus mirror + `snapshots.enabled=true`（父 POM 是 SNAPSHOT，不开则解析不到），已验证 `quectel-code-parent` 可拉取 |
| 门禁脚本 | ❌ 本地不存在 | 跳过（见 D6） |

内网连通性已实测：Nexus `192.168.10.107:8081`、GitLab `git.quectel.com`、网关 `192.168.10.27:8088`、MySQL `192.168.10.28:3306`、MinIO `oss-dev.quectel.com` 均可达。

---

## 7. 验收标准

1. `cd demo-new-service && mvn compile` → `BUILD SUCCESS`（编译验证循环，最多 5 轮）
2. `mvn spring-boot:run` → 日志出现 `Started DemoNewServiceApplication in X.XXXs`（启动验证循环，最多 5 轮）
3. `cd demo-new-web && pnpm dev:web-app` → 出现 `Local:` / `ready in`，页面可访问
4. 附件页在 **mock 模式**下列表正常渲染、分页可翻到第二页
5. 切 **real 模式**后，上传一个文件 → OSS 有对象、`sys_attachment` 有记录、列表出现该行、预签名 URL 可下载、删除后列表消失
6. 验证方式用 **DevTools Network**，**不用 curl**（curl 打的是 dev-server 代理，绕过了浏览器内的 Guard）

---

## 8. 已知风险

| 风险 | 影响 | 缓解 |
|------|------|------|
| **前端 SSO 无文档** | 真实登录链路可能跑不通 | 开发期走 whiteList/mock token；联调时实测；必要时向框架团队索取 QMonoX 文档 |
| 门禁脚本缺失 | env-lock 等 AI-First 门禁不生效 | 人工遵守 env 红线；后续拿到 skill 包补装 |
| 知识库自身矛盾 | 照文档写可能撞车 | 已识别两处：① `detail.vue` vs `detail/index.vue`（`routes.md` 为准）；② `coding-standard.md:269-287` 的 mock 分支写法已被 `api-rules/` 判为错误（以 `api-rules/` 为准）；③ `components.json` 的 configSnippet 已过时（以 `项目骨架模板.md` 为准） |
| pnpm 全局降级 | 可能影响用户其他项目 | 已获用户明确同意 |
| MinIO bucket `quectel-cli` 可能不存在 | 上传 500 | 首次上传前确认 bucket；不存在则建或换名 |
