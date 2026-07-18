# 全程用 AI + Quectel-code 组件 Skill 做 0→1 项目 · 方法论复盘

> 一次真实实验的复盘：**不写一行手工代码，全程由 AI 结合公司内部 Quectel-code 三件套（code-master / vue-coding / java-coding）从零搭起一个前后端项目**，验证可行性并沉淀方法论。
> 载体是一个内部示例项目（前端 QMonoX + 后端 Spring Boot，SSO 走 UAA），本文**只讲方法与踩坑，不涉及具体业务需求**。

---

## 一、结论先行（TL;DR）

**可行——但有一个决定成败的前提：把企业内部框架当"黑盒"，严格遵循 skill，而不是让 AI 凭开源通用知识自由发挥。**

- ✅ AI 能完成：环境搭建、脚手架、启动脚本、前后端联调配置、设计文档生成、按规范建页面、门禁自检自修复。
- ⚠️ AI 最大的风险不是"不会写"，而是**"自作主张"**——用它训练来的通用 Vue/Spring 知识去改企业框架的黑盒配置，看似合理，实则违反内部约定，直接导致**白屏 / 全 404 / 权限 500**。
- 🔑 让"全程用 AI"安全落地的，正是 **skill 里的门禁（gate-check / self-check / env-lock）+ 设计文档驱动流程**。门禁是真相来源，AI 的直觉不是。

> 一句话方法论：**AI 负责"高速执行"，skill 门禁负责"守住红线"，人负责"在冲突点拍板"。**

---

## 二、正确的主线流程（可照抄）

Quectel-code 套件是**场景驱动 + 设计文档驱动**的。标准 0→1 主线：

```
0. 套件与环境就位
   └─ 安装 code-master / vue-coding / java-coding（内网 marketplace）
   └─ 内网 git 需绕外部代理（NO_PROXY）+ token 走 gitconfig insteadOf

1. 入场体检（任何场景开工前必跑，别跳）
   └─ permissions-sync   → 一次授权，后续零确认
   └─ version-check      → 套件是否有更新
   └─ reference-sync     → 把权威规范拉进 .quectel-code/cache/reference/（不 sync 就没规范＝瞎猜）
   └─ self-check         → 存量基线门禁

2. 场景识别 → 读对应规范文档
   └─ 创建项目 / 原型转代码 / 新建模块 / 改页 / 联调 / 仅验证 —— 各有专属执行手册

3. 设计文档驱动（0→1 的核心，最容易被 AI 跳过）
   └─ prototype-to-code STEP 00→04：
      workspace 初始化 → 扫原型 → 提实体 entities.json → 原型分析报告(暂停确认)
      → 生成《前端详细设计文档》(INDEX.md 含"开发进度表"=排期 + 各模块设计 + API 文档)(暂停确认)
   └─ ⛔ 排期来自 STEP 04 的 INDEX.md 开发进度表，不是 AI 拍脑袋手排

4. 服务上下文成对锁定（STEP 04.6）
   └─ 前端 VITE_API_DEFAULT_SERVICE_BASE_URL ⇄ 后端 context-path，第一段必须一致

5. 逐页/逐模块开发（STEP 05→13）
   └─ 路由 → API/Adapter 防腐层 → types → 列表/表单/详情/自定义页 → 联调 → typecheck

6. 自修复门禁铁律（每写完一段代码）
   └─ 跑 self-check，FAIL 自己修到 PASS 才交付，绝不把 FAIL 抛给人
```

---

## 三、关键红线与踩坑复盘（本文精华）

以下每一条都是本次实验中 **AI 真实踩过、被门禁或人拦下** 的坑。反面案例最有教育意义。

### 红线 1 · env 是黑盒，只能改白名单

- **规则**：根 `env/.env` **只允许改 `VITE_API_DEFAULT_SERVICE_BASE_URL` 一个变量**；`apps/*/env/` 下**一字不能动**；其余所有 `VITE_*` 禁改。
- **翻车现场**：AI 为了"做一个非 SSO 账密登录页"，自作主张把 `VITE_USE_ENV_AS_FRONT_END_APP_CONF` 改成 Y、注释掉 `VITE_APP_ID`、关掉 SSO——**这些全是禁改项，直接白屏 + 全 404**。
- **为什么会犯**：这套改法用开源 Vite 知识看"完全合理"，但它违反企业框架的黑盒约定（框架 SSO 已集成、APP_ID 是权限系统识别应用的钥匙）。
- **兜底机制**：`gate-check --gate=env-lock-verify` 门禁（`.quectel-code/state/env.lock` 存 hash）会立即判"env 被篡改"。
- **正确修复**：`git checkout HEAD -- <env文件>` 还原 pristine（env 被 git 跟踪），改白名单变量后重跑 `env-lock-create` 固化。

> 教训：**框架配置=黑盒，除 skill 明示的白名单项外，一个字都别动。**

### 红线 2 · 设计文档驱动，不能跳步手排

- **规则**：0→1 必须走 prototype-to-code STEP 00→04 生成"前端详细设计文档"，它是后续代码生成的**唯一依据**；排期＝ STEP 04 INDEX.md 的开发进度表。
- **翻车现场**：AI 直接读上游产品设计（IA 文档）+ 复杂度启发式**手排了一份开发计划**，跳过了 skill 的 STEP 00→04。看着像那么回事，实则"凭空发挥"，正是设计驱动要消灭的。
- **正确做法**：老实跑 STEP 00→04 产出 `docs/<项目>-前端详细设计文档/`（INDEX + 各模块设计 + API 文档），排期从进度表里出。

### 红线 3 · definePage / 路由铁律

- 只有一级菜单页 `layout:'default'`，**所有子级页 `layout:false`**；没有带 `layout:default` 的父分组 → `genRoutes()` 静默丢路由 → **全 404**。
- `definePage.name` 与 `defineOptions.name` **必须一致**；`title` 写**纯 locale key**（禁 `page.` 前缀、禁翻译函数包裹，框架自动拼）。
- 菜单图标三步缺一不可：`icons.mjs --has` 验证存在 → 加 `uno.config.ts` presetIcon.safelist → dir.meta.yaml 引用（漏 safelist＝图标不显示）。
- 框架内置页（`index.vue` 重定向桩 / `login/` / `error-pages/`）**禁改**。

### 红线 4 · 前后端联调＝三件成对

- 前端 `VITE_API_DEFAULT_SERVICE_BASE_URL=/xxx/` ⇄ 后端 `context-path=/xxx` ⇄ 本地 `q-cli.config.ts` 代理 `/api/xxx → localhost:后端端口`（rewrite 去 `/api`）。
- 诊断定式：直接 `curl --noproxy '*' localhost:<后端>/xxx/me`，返 `{"code":401,"msg":"Missing Authorization header"}` 即"后端校验层就绪、在等 token"——那卡点必在"前端拿 token"这半，而非服务端校验。

### 红线 5 · SSO 是框架自带，别乱配

- 框架已集成 SSO（UAA/OAuth2.0）。**除非专门调试 SSO，否则不要动 `VITE_DEV_SSO_*` 等配置**。localhost 拿不到 token 是回调/跨站 cookie 问题，不是"要你去手搓登录态"。

### 元规则 · 冲突先问，不回则按 skill

> **skill 与用户指令冲突时，先问人；人不回复，则严格按 skill 执行。不自由裁决。**

---

## 四、给"用 AI 干活"的人的协作姿势

| 姿势 | 说明 |
|---|---|
| **把 skill 当权威，把 AI 的通用知识当"待验证假设"** | 企业黑盒框架里，开源直觉经常是错的。以门禁/规范为准。 |
| **门禁即真相** | `gate-check` / `self-check` / `typecheck` 说 PASS 才叫过，AI 说"应该没问题"不算数。 |
| **系统化调试，找根因不猜** | 多组件系统先加观测（curl 直连/走代理对比、看 401 响应体）定位是哪一层坏，再动手。 |
| **写完必自检自修复** | 自修复铁律：self-check FAIL 自己修到 PASS，别把红字丢给人。 |
| **大批量生成并行化** | 如一次要出 6 个模块的设计文档，派多个子 agent 并行（各只读自己模块的 PRD+原型），比串行省时省钱；产出后逐份校验保真。 |
| **保真不臆造** | 字段/枚举/文案逐字抄设计源；源里没有的标注"待补/待确认"，绝不编。 |

---

## 五、可复用检查清单（开工即用）

**开工前**
- [ ] 套件已装、内网 git 通（NO_PROXY + token insteadOf）
- [ ] 跑 permissions-sync / version-check / reference-sync（reference 缓存就位）
- [ ] self-check 基线通过（存量违规先标记）

**设计阶段**
- [ ] 走 prototype-to-code STEP 00→04，产出前端详细设计文档 + API 文档
- [ ] 排期取自 INDEX.md 开发进度表（不手排）
- [ ] STEP 04.6：service-base-url ⇄ 后端 context-path 成对锁定 + env-lock-create

**编码阶段（每页/每模块）**
- [ ] 只碰该场景规范允许的文件；env/框架内置页不动
- [ ] definePage 铁律（layout/name 一致/纯 title key）+ 图标三步
- [ ] 写完跑 self-check → PASS；最终 `pnpm typecheck` → 0 error（dev 不查类型，别拿它当编译验证）

**红线自问**
- [ ] 我要改的这行 env / 配置，是 skill 白名单里的吗？（不是就别改）
- [ ] 这个结论是门禁/规范背书的，还是我"觉得"的？

---

## 六、效率与成本的诚实备注

- **成本主要花在两处**：① AI 自作主张翻车后的**返工**（改 env→白屏→排查→还原）；② 大规模**设计文档生成**（多模块、字段字典逐字保真）。
- **省钱要点**：
  - 前期就把姿势立对（严格按 skill），能消掉绝大部分返工；本次一半以上开销是"先犯错再纠正"造成的，本可避免。
  - 大生成任务并行化 + 每个子 agent 只喂它需要的最小上下文。
  - 诊断用最小可复现（curl 直连比盲改配置便宜得多）。
- **可推广性**：这套"入场体检 → 场景驱动 → 设计文档驱动 → 逐模块开发 → 门禁自修复"的主线，对 code-master/vue-coding/java-coding 三件套通用，可作为团队 0→1 的标准 SOP。

---

## 七、一页纸总纲

> **AI 提供马力，skill 门禁提供刹车与护栏，人在冲突点掌方向盘。**
> 全程用 AI 做企业 0→1 项目**能成**，前提是：**把框架当黑盒、把 skill 当权威、把设计文档当唯一依据、把门禁当真相**——而不是让 AI 用开源经验自由发挥。
