# 用 Quectel-code 套件 + AI 快速开发 · 正确姿势

> 适用对象：**已了解公司前后端框架与开发规范**的研发。
> 目标：用 `code-master / vue-coding / java-coding` 三个 AI Skill，把"产品设计 → 前端设计 → 后端设计 → 前后端开发 → 联调 → 测试 → 上线"跑成一条可控流水线，研发时间最多可省 80%。
> 本文以「销售商机互助平台」骨架搭建为实战样本，把新手容易走的弯路反过来写成**开场即说清**的正确做法。

---

## 0. 一句话心法

**前端先行定义 API 契约 → 后端以契约为准开发 → 联调以 Swagger 为基准自动差异修复。**
全程由 `code-master` 调度，`.quectel-code/state/project-state.json` 状态文件驱动，断点可续、阶段可回退。你从"专项开发者"转成**架构师/审查官**视角：大多数时间是给 AI 指方向、做 check，而不是自己写代码。

---

## 1. 一次性准备（做一次，后面都省事）

### 1.1 环境（版本要卡准，别用最新）

| 工具 | 要求 | 易错点 |
|------|------|--------|
| Node.js | **22.x（≥22.8）** | 别用 24；vite 编译可能出问题 |
| pnpm | **10.x（必须 < 11）** | ⚠ 装了 pnpm 11 前端脚手架会拒绝，`npm i -g pnpm@10` |
| JDK | 1.8 | |
| Maven | 3.6.3+（配好公司 Nexus 私服 `settings.xml`） | `quectel-code-parent` 靠私服解析 |
| Git | ≥ 2.30 | |
| q-cli | 由 vue-coding 自动装（内部 registry） | |

### 1.2 GITLAB_TOKEN（必要）

Skill 要从内部 GitLab（project 1491 知识库）拉规范文档/组件库。
GitLab → Settings → Access Tokens → 勾 `read_repository` → 创建，然后：
```bash
setx GITLAB_TOKEN "你的token"     # Windows；配完重启会话生效
```
> 如本机装了走外网的全局代理（`HTTP_PROXY`），需把 `git.quectel.com` 加进 `NO_PROXY`，否则内网 git 会被代理拦到拦截页。内网 git 认证可用 `git config --global url."http://oauth2:$GITLAB_TOKEN@git.quectel.com/".insteadOf "http://git.quectel.com/"`。

### 1.3 安装三个 Skill（手册 2.1 · Claude CLI 推荐）

```bash
/plugin marketplace add http://git.quectel.com/quectel-code/quectel-plugin-marketplace.git
/plugin install code-master
/plugin install java-coding
/plugin install vue-coding
/reload-plugins        # 未立即生效时
```
装完**重启 Claude Code**，三个 skill 才作为正式能力加载。

### 1.4 ⚠ 项目必须放**纯英文（ASCII）路径**

QMonoX 前端 self-check 门禁会拿**页面绝对路径**匹配中文——项目路径里只要有一个中文字（如 `大模型提效`），门禁就永久报「文件名含中文」，`pnpm dev` 起不来。
**正确做法：一开始就把工作目录放在纯英文路径**，例如 `C:\dev\demo-new\` 或 `C:\Users\xxx\Desktop\dmx\`。

---

## 2. 标准 Workspace 与六阶段

```
{workdir}/                          ← 纯英文路径
├── {project}-design/               # PRD 仓库（git clone）
├── {project}-web/                  # 前端（vue-coding / q-cli 建）
├── {project}-server/               # 后端（java-coding 建）
├── docs/                           # 各阶段设计文档产物
└── .quectel-code/state/project-state.json   # 进度真相源
```

| 阶段 | 谁做 | 产物 |
|------|------|------|
| 1 产品设计 | 你/PM | PRD、原型、需求分析 → `docs/` |
| 2 前端设计 | **vue-coding** | 需求原型分析、前端详细设计、**API 接口文档（前后端契约）** |
| 3 后端设计 | **java-coding** | 后端详细设计、DDL SQL |
| 4 前后端开发（可并行） | **vue-coding + java-coding** | Vue 页面/组件；Java 骨架+业务代码 |
| 4.5 联调 | **vue-coding integration** | Swagger diff → 自动修复 → 冒烟 |
| 5 测试 / 6 上线 | 你 | 测试报告、部署配置 |

---

## 3. 正确的对话流程（关键：让 code-master 调度，你只做决策）

### 开场（就这一句）
> **"我有一个『销售商机互助平台』系统，需求设计在 https://git.quectel.com/xxx ，用 code-master 帮我开发。"**

code-master 会初始化 `project-state.json`、识别当前阶段、按阶段依次调度 vue-coding / java-coding。**你不用记命令，跟着它的提问答即可。**

### 每阶段你要做的
- **答它的选择题**（组件选型、模块拆分等）——见第 4 节把关键决策提前说清。
- **每个 Step 让它输出边界确认**：`[step X 完成] 引用规则文档 / 产物 / 下一步`，确认后再进下一步。
- **不确定就让它停下问你**，别让它猜框架内部 API。

### 只搭框架骨架（需求未就位时）
> **"需求还没就位，先用 java-coding 建后端骨架、vue-coding 建前端骨架，要支持 SSO 单点登录和获取用户信息。"**

---

## 4. 开场即说清的关键决策（这是避免弯路的精华）

新手最容易在这几点上返工。**在对应阶段一开始就主动说清**：

1. **要 SSO/security，就同时选 mysql 组件。**
   后端 `security-starter` 内部依赖 MyBatis-Plus，**只选 web+security 不选 mysql 会编译过、但一启动就 `NoClassDefFoundError`**。
   > 正确话术："后端组件选 web + security + mysql；用框架 `quectel-code-security-starter` 接 UAA，示例接口 `/me` 用 `SecurityUtils.getCurrentUser()` 取当前用户。"

2. **暂时没有数据库？告诉 AI"不连库走内存"。**（手册 FAQ 5.4）
   出于安全，skill 默认不直连数据库。骨架阶段可让它走内存；项目跑起来后再给数据库信息让它对接。
   > 或：明确给 dev 库信息，JDBC url 加 `createDatabaseIfNotExist=true` 让首启自建库。

3. **环境固定说清**（dev/test/prod）。SSO 的 UAA 地址、Nacos、数据源按环境不同，一开始说"用开发环境"，它就取 dev 值，省得来回改。

4. **前端让 q-cli 建，禁止手搭。** 组件（QBigTable/QForm 等）从 GitLab 下载，依赖装进 `apps/web-app` 工作区（不是 monorepo 根）。这些 vue-coding 会自动做，你别自己动手拼。

5. **单体 vs 微服务**：要注册 Nacos/网关就选 `cloud` 组件；只是本地骨架就先不选，避免依赖 Nacos 才能启动。

---

## 5. 高频坑位 → 正确姿势（速查）

| 坑 | 现象 | 正确姿势 |
|----|------|----------|
| 项目路径含中文 | 前端 `pnpm dev` 门禁报「文件名含中文」永不过 | 项目放纯 ASCII 路径 |
| security 不带 mysql | 编译过、启动 `NoClassDefFoundError: MetaObjectHandler` | security 场景必带 mysql-starter |
| pnpm 版本 11 | q-cli / 脚手架报错 | 锁 pnpm 10.x |
| 依赖装到 monorepo 根 | 组件报缺依赖 | 装进 `apps/web-app` 工作区 |
| clean-demo 没删干净 | self-check 因残留 demo 页 FAIL | 手删 `demos/external-link/home/with-badge` 等脚手架演示页 |
| 内网 git/registry 被代理拦 | clone 跳到拦截页 | `NO_PROXY` 加 `git.quectel.com`；token 走 `insteadOf` |
| 装完 skill 没重启 | skill 不生效 | 重启 Claude Code / `/reload-plugins` |
| AI 猜框架内部 API | 用 antdv/Spring 通用知识硬套出错 | 让它先查 `{starter}-starter.md` / constraints / codegraph，拿不到就停下问你 |

---

## 6. 一个完整的"正确版"示范对话（骨架搭建）

> 以下是如果一开始就懂规范、**没有弯路**的理想流程。

1. **你**：（在纯英文路径 `C:\dev\demo-new` 打开 Claude Code，已装好 skill 并重启）
   "用 code-master 帮我搭『销售商机互助平台』前后端框架骨架，前端 vue3+ts+vite，后端 Java；要支持 SSO 单点登录和获取用户信息，需求未就位先搭架子。"

2. **code-master**：初始化状态 → 识别为 Phase 4 骨架 → 调度 java-coding + vue-coding，让你确认组件。

3. **你**（一次说清关键决策）：
   "后端组件 web + security + mysql，单体不接 Nacos；SSO 用 quectel-code-security-starter 接 dev 环境 UAA，加个 `/me` 示例接口用 SecurityUtils.getCurrentUser() 取当前用户；dev MySQL 用 createDatabaseIfNotExist 自建库。前端用 q-cli 建 QMonoX 骨架。"

4. **java-coding**：读 `init-backend.md` + `security-starter.md` → 生成 pom（继承 quectel-code-parent，不写版本号）/启动类/application.yml/`/me` 接口 → `mvn compile` + `spring-boot:run` 验证启动。

5. **vue-coding**：读 `create-project.md` → q-cli create + add web-app + 装依赖 + 下组件 + 注册 Antd（在 QUI 前）→ `pnpm dev` self-check PASS。

6. **你**：确认两边都起来（后端 :8081，前端 :8080/web），骨架交付。需求就位后回到 Phase 1/2 让 code-master 继续。

**产出**：可编译、可启动、符合部门框架规范的前后端骨架，SSO 打通、能取用户信息。

---

## 7. 交付验收标准（每阶段 check 什么）

- **后端**：`mvn compile` BUILD SUCCESS；`mvn spring-boot:run` 出现 `Started ...Application`；Swagger `/doc.html` 可访问；`/me` 带 UAA token 能取到用户。
- **前端**：`pnpm dev:web-app` 出现 `Local: ...`，self-check `FAIL: 0`；页面 F12 接口调用正确、数据回显正常。
- **联调**：以后端 Swagger 为基准做 diff，snake_case↔camelCase、字段差异由 vue-coding 的 integration 流程自动修复。
- **端口**：后端与前端 dev 默认都 8080，本机同起时把后端错开到 8081。

---

> 记住：AI 写错、理解错很正常——你以**把控者**身份指挥它改就行。真正的提效不在"让 AI 少犯错"，而在你用架构师视角把关键决策**提前说清**、让 skill 严格按公司规范执行。
