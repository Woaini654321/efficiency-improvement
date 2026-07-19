# 销售商机互助平台 前端实现 BUILD-PLAN（施工手册 / 续跑上下文）

> 用途：原型转代码任务的**续跑锚点**。上下文压缩后，先读本文件即可继续，无需重读全部 skill 规范。
> 任务：把 `PRD/商机平台/living-docs/prototypes/` 全部页面用公司 vue-coding skill + `@q-ui/web`/antdv 实现为前端页面（数据后置，mock 展示）。无人值守，能决策则用推荐方案。
> 用户硬约束：**同一个展示字段只来源一张表**（前端 types 扁平化，用冗余快照字段，绝不跨表拼装）。

## 0. 环境与命令（关键路径）
- 项目根：`C:/Users/leon.yan/Desktop/dmx/demo-new/sales-lead-hub-web`（含 package.json / pnpm-workspace.yaml）
- app 源码根：`apps/web-app/src`
- SKILL_DIR：`C:/Users/leon.yan/.claude/plugins/cache/quectel-plugin-marketplace/vue-coding/4.5.1`
- reference 缓存：`.quectel-code/cache/reference/`（23 份规范，已 sync）
- 门禁（写完必跑到 PASS，在项目根执行）：
  - `node "$SD/cli/self-check.mjs"`（predev 钩子会跑；FAIL 则 vite 不起）
  - `pnpm typecheck:all`（业务源码类型检查，唯一能抓类型错的；根 `pnpm typecheck` 假绿勿用）
  - icons：`node "$SD/cli/icons.mjs" --has <name>` / `--grep linear`
- Step0 前置体检已过：permissions-sync/version-check/reference-sync/self-check 全 PASS（FAIL0 WARN0）。
- env 已配好 `VITE_API_DEFAULT_SERVICE_BASE_URL=/sales-lead-hub/`（STEP04.6 已满足，勿动 env）。

## 1. 数据模型（SSOT）
权威来源：`PRD/商机平台/产品文档-v2.0/`（01_全局规约手册 §4 ER/§10 枚举/§1状态机/§2 SLA）+ `决策纪要与修正基线.md`(D1~D9/TC01~05 覆盖一切)。
15 核心实体=15 张表：users, departments, product_lines, product_line_members, categories, opportunities, opportunity_requests, solution_responses, interactions, notifications, notification_batches, subscriptions, view_logs, audit_logs, announcements。join: opportunity_category, request_category, alerts。
**一字段一表**：发布人姓名`publisherName`/发布部门`publisherDeptName`/响应人`responderName`/触发人`triggerUserName`/操作人`operatorName` 一律**冗余快照列**落在各自实体表；分类名/产品线名走各自 lookup 表（categories/product_lines 独占）。计数(view/like/collect/comment/response)已是实体行冗余计数=单表。priority/slaRemaining 由 urgency+created_at 派生不入库。
关键枚举/状态见 PRD；SLA 首响：critical 2h/urgent 4h/normal 24h，固定 L1→L2→L3，收到首个方案即停表。评论≤2级。商机 status: draft/published/archived；需求 status: Pending/Collecting/Adopted/Closed；公告(采PC24) draft/scheduled/published/withdrawn。

## 2. 模块 → 目录/图标/页面 映射（12 顶级菜单）
| 顺序 | module(kebab) | 图标(已验证存在) | 页面 | 原型 |
|--|--|--|--|--|
|0|`home`|home-linear|index(仪表盘)|PC-01 首页/找方案|
|1|`opportunity`|bulb-linear|index(查方案列表)/detail/form|PC-17/02/03|
|2|`requirement`|document-linear|index(提需求)/detail/form|PC-04/05/06|
|3|`notification`|notice-linear|index(通知中心)/preference/announcement-detail|PC-07/15通知偏好/16|
|4|`profile`|bookmark-linear|index(单页, menu可false)|PC-08|
|20|`operation`|settings-linear|组:audit/category/dashboard/sla/log/announce/batch|PC-10/11/12/13/14/15公告/23|
|30|`discussion`|discuss-linear|index/detail/post|PC-18/18-1/18-2|
|31|`feedback`|like-linear|index(单页)|PC-19 吐槽墙|
|32|`intel`|search-linear|index/competitor-detail/industry-detail/submit|PC-20情报/20-1/20-2/20-3|
|33|`tool`|shop-linear|index(单页)|PC-20 工具助手|
|34|`meeting`|calendar-linear|index(单页)|PC-21|
|35|`task`|point-list-linear|index(单页)|PC-22|

- operation 组：`pages/operation/dir.meta.yaml`(layout default, order 20, title `operation.DEFAULT`, menu.icon settings-linear)；子页 `pages/operation/{audit,category,dashboard,sla,log,announce,batch}/index.vue`（layout false, menu true, title `operation.audit.DEFAULT`…**段内不能含连字符**故用单词）。
- 其余顶级组：各自 `pages/{module}/dir.meta.yaml`(layout default, 对应 order/title `{module}.DEFAULT`/icon) + `index.vue`(layout false, menu true, title `{module}-list` 或形态B)。单页模块(feedback/tool/meeting/task/profile)只有 index.vue，可用形态A(顶级独立页, title 单段 kebab, layout 'default')**无需 dir.meta.yaml**。
- 详情页统一 `detail/index.vue`（禁平级 detail.vue，routes.md 权威），`route.query.id`。

## 3. 每模块产出（文件组织）
```
src/apis/{module}/types.ts            # DTO(snake)+ViewModel(camel,ID全string)+Params+Result(用@q-mono-x/types/base 的 PaginationParams/PaginationResult)
src/apis/{module}/mocks/{module}.json # DTO(snake_case) ≥12条, ID字符串, 含null/多状态
src/apis/{module}/{module}Adapter.ts  # adapter(raw已解包,勿.data.;?? 兜底不写'--')+ AIRequestGuard.register({viewSchema:()=>adapter(mockData), adapter})
src/apis/{module}/{module}Api.ts      # 查询类用 (await AIRequestGuard({adapter, request:()=>request.POST/GET})) as T；增删改直接 request.POST
src/pages/{module}/...                # 页面
```
locale：`src/locale/zh-CN/index.ts` & `en-US/index.ts` 同步加 `page.{module}` + 模块业务 namespace；common 已在 skill 规范列出必备字段。**page.{module} 的 module 必须 kebab 原值**。

## 4. 组件/写法铁律（高频门禁点）
- **列表**：`<QBigTable>`(禁 a-table)。`import { QBigTable } from '@/components/q-big-table'`。列用 `field/title/slots`(禁 dataIndex/customRender)。`queryApi` 双参 `({page},searchParams)` 返回 `{result, page:{total}}`。searchConfig 是 FormSchema[]，只认 field/label/component/**componentProps**(禁 props/colProps)，定宽用 `componentProps.style:'width:140px'`+`allowClear:true`。height="100%"。toolbarConfig 数组。wrapper class 固定 `h-full p-[16px] bg-white rounded`。用 JSX slot 时 `<script setup lang="tsx">`。a-tag 颜色仅 blue/red/green/orange/default。
- **表单**：`<QForm :schemas v-model:model>`(禁 a-form)。字段 componentProps。Switch 的 valuePropName:'checked'+updateEventName:'update:checked' 放 schema 顶层。QUpload 用 manual 布尔(无 mode)。QRemoteSelect 在 QForm 内必须 `render:(model,setFormModel)=>h(QRemoteSelect,{class:'w-full',modelValue,'onUpdate:modelValue',pageApi,fieldNames})`。删除二次确认 a-popconfirm placement="topRight"。Modal 宽度对照原型。
- **详情**：`a-descriptions bordered size="small" :column`。空数据 `<Empty type="noData"/>`(from @q-web-plugin/empty)。附件 `QFileList`。空值 `?? '--'` 在模板不在 adapter。
- **路由 definePage 5铁律**：name 顶层且=defineOptions.name；layout/menu/title 进 meta+`satisfies RouteMeta`；顶级 layout 'default' 子级 false；title 纯 locale key(禁 page. 前缀/禁 $t())；authCode/keepAlive 保持注释。`definePage` 是宏禁 import，只 `import type {RouteMeta} from 'vue-router'`。
- **CSS**：原型 var(--accent)→hsl(var(--primary))、--border→--line、--muted→--secondary-text、--surface→--card-bg、--danger→--error。tokens 是 HSL 裸值必 hsl() 包裹。禁行内 style，用 UnoCSS class / `<style scoped>`。i18n options 用 computed 包裹。禁硬编码中文。
- **request 签名**（Request.d.ts 权威，仅7方法无 DELETEURL）：GET(opt,query) 第二参就是 query 别包 {params}；POST(opt,data) 只2参；PUT(opt,data,params)；GETURL(opt,id,params)；DELETE(opt,id)。分页列表用 `request.POST({url:'{module}/page'}, params)`。
- 菜单图标三步：icons --has 确认 + uno.config.ts safelist 追加 + dir.meta.yaml 引用。**safelist 已加**本任务 11 个模块图标。
- **不改**：env/**、pages/login、pages/error-pages、pages/index.vue(方案1重定向)、env.lock。

## 5.1 完成状态（2026-07-18 已交付）
**全部 30 个业务页面 + 17 个 api 模块已实现，self-check PASS(0/0)，业务代码 typecheck 0 error。**
- 交付方式：opportunity 打通全流程为样板 → 6 个并行 subagent 镜像样板生成其余 11 模块 → 中心化合并 locale 片段 → 集中跑门禁修偏差。
- 门禁修复中沉淀的坑（复用）：
  1. `request.POST(data)` 的参数类型必须 `type` 别名（非 interface），否则缺索引签名不匹配 `BodyArg`。
  2. `exactOptionalPropertyTypes` 下，可选字段若会被赋 `undefined`，类型要写 `field?: T | undefined`（转义）；或调用处条件展开 `...(x?{k:x}:{})` 不传 undefined。
  3. 原生 `<a-select>` self-check 要求 `:value`+`@update:value`，不能 `v-model:value`（其它组件 v-model 可以）。
  4. `<a-form>/<a-form-item>` 被门禁禁止（除非走 QForm）——简单弹窗表单改用 `<div>`+label 布局。
  5. `noUncheckedIndexedAccess`：`Record<string,T>` 索引得 `T|undefined`；已知键集合改用字面量联合 `Record<(typeof arr)[number], T>` 消除。
  6. QBigTable `toolbar/columns` 标注 `ToolbarButton[]/TableColumn[]`（从 '@/components/q-big-table' 导入）避免字面量类型收窄报错；`:query-api` 可选，纯 `:data` 时不要传 `null`。
- 已知遗留（非本次引入）：scaffold 组件 `components/{q-form,q-upload,q-big-table,q-file-list}` 与个别 node_modules 在 `exactOptionalPropertyTypes` 下有基线 type error → `pnpm build:web-app`(vue-tsc) 会红；但 `pnpm dev:web-app`(esbuild predev self-check 已过) 可正常起。需要绿色生产构建再单独处理 scaffold 基线。

## 5. 进度
- [x] Step0 体检 / 读全部规范 / PRD 数据模型(子agent) / 原型页面类型
- [x] 地基：`apis/guard-setup.ts`(mock DEV) + main.ts 引入 + uno.config.ts safelist 加 11 图标
- [ ] 原型 UI 细节：子agent a547561237f273e63 正在产出每页 UI spec（layout/表格列/筛选/表单/交互/精确中文文案）——完成后存档到本目录 `原型UI-spec.md`
- [ ] 待建：locale 地基(common+home) → home 模块 → opportunity 全流程跑通门禁 → 其余模块批量 → 最终 typecheck:all + 全量 gate
- 任务表 TaskList #1~#7；先 opportunity 打通再复制。
