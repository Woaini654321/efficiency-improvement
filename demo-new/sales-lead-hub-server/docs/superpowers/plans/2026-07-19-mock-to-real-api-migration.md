# 全模块 Mock → 真实接口 开发方案

> **For agentic workers:** REQUIRED SUB-SKILL: 用 `superpowers:subagent-driven-development` 或 `superpowers:executing-plans` 逐任务执行。步骤用 `- [ ]` 复选框跟踪。
> 后端写码前必读 `java-coding` skill；前端每次写完 `.vue`/`.ts` 必自跑 `vue-coding` 的 self-check 到 PASS。

**Goal:** 把销售商机互助平台剩余 64 个接口从本地 mock 切到真实服务端，复用 requirement 竖切已验证的模板。

**Architecture:** requirement 模块（4 接口）已跑通完整竖切并经真实 SSO 联调验证，其 12 类文件构成可复制的「竖切配方」。本方案先做一次性横切地基（消除每模块都会重踩的坑），再按「同构度」从高到低批量复制配方，最后处理无表模块。

**Tech Stack:** Spring Boot 2.7.18 / Java 8 / MyBatis-Plus 3.5.16 / MySQL 8（远程共享库 192.168.10.28:3306/sales_lead_hub_server）；Vue3 + TS + Vite + pnpm（QMonoX）。

---

## Global Constraints

逐条来自 CLAUDE.md 与已验证的框架黑盒约定，**每个任务都隐含包含本节**：

- 回复一律中文；Commit 用中文 Conventional Commits；**禁止 AI 自动 git commit/push**，提交由人工执行。
- `env` 是黑盒：只有根 `env/.env` 的 `VITE_API_DEFAULT_SERVICE_BASE_URL` 可改，`apps/*/env` 一字不动。
- 不提交明文口令；DB 口令只放 `application-local.yml`（已 gitignored）。
- 项目路径必须纯 ASCII。
- PRD 是产品团队 SSOT，**不擅自 edit §10 枚举**。
- 禁定义业务侧 `MetaObjectHandler`；审计字段 `createTime/updateTime/createBy/updateBy` 由框架 `SecurityMetaObjectHandler` 填，业务禁手动赋值。
- 逻辑删除字段是 `deleted CHAR(1) 'Y'/'N'`，`@TableLogic(value="N", delval="Y")`。
- 主键统一 `BIGINT + IdType.ASSIGN_ID`；前端 ID 全 string 由全局 Jackson `Long→String` 满足。
- 业务角色**禁用** `@PreAuthorize` + UAA 角色（UAA 无业务角色），一律走 `CurrentUserResolver.requireAnyRole(...)`，fail-closed。
- 权限拒绝抛 `BaseException(ErrorCode.FORBIDDEN, msg)`，**禁抛** `AccessDeniedException`（会被兜成 500）。
- VO 出参 snake_case 用**类级** `@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)`；**禁设全局** `PropertyNamingStrategy`（双向，会让入参 camelCase 静默变 null）。
- VO 的 `LocalDateTime` 字段必须加 `@JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")`。
- 分页 `pageSize` 上限 500（框架 `maxLimit=500` 静默截断），DTO 加 `@Min(1) @Max(500)`。
- 后端启动只用 `./start-server.ps1`（含固定端口 + 递归杀进程树），**禁直接** `mvn spring-boot:run`。
- 离线门禁：`mvn test`（默认 `excludedGroups=integration`）必须全绿；联调门禁：`mvn test -Dgroups=integration`。
- 前端门禁：`pnpm typecheck:all` + `node <SKILL_DIR>/cli/self-check.mjs` 必须 PASS。

---

## 现状盘点（方案的事实基础）

**接口总数 68，已完成 4（requirement），剩余 64。**

### 有表模块（44 接口）— DO 已建齐，缺 Controller/Service/DAO

| 模块 | 接口数 | 接口 | 支撑表 |
|---|---|---|---|
| opportunity | 6 | page/detail/create/update/changeStatus/delete | `opportunity` + `opportunity_category` |
| announce | 9 | `announcement/{page,detail}` + `operation/announce/{page,detail,create,update,delete,changeStatus,stats}` | `announcement` |
| category | 5 | list/create/update/delete/changeActive | `category` |
| member | 4 | member/`member/{page,add,update}` | `product_line_member` + `product_line` |
| notification | 4 | page/markRead/markAllRead/preference/save | `notification` + `subscription` |
| sla | 4 | `operation/sla/{page,meta,stats,urge}` | `opportunity_request`（SLA 字段派生） |
| audit | 4 | page/changeStatus/changePin/delete | `opportunity` + `opportunity_request` |
| interaction | 3 | comment/comments/like | `interaction` |
| auditLog | 1 | page | `audit_log` |
| employee | 1 | page | `sys_user` |
| profile | 1 | `profile/center` | `sys_user` + `interaction` + `subscription` 聚合 |
| home | 1 | `home/dashboard` | 多表聚合统计 |
| dashboard | 1 | `operation/dashboard` | 多表聚合统计 |

### 无表模块（20 接口）— ⛔ schema.sql 中无对应表

| 模块 | 接口数 | 缺的表 |
|---|---|---|
| intel | 5 | 竞品情报、行业情报（2 张） |
| task | 4 | 会议任务（1 张，与 meeting/batch 共用） |
| meeting | 3 | 会议（1 张） |
| feedback | 3 | 匿名反馈（1 张） |
| discussion | 3 | 讨论帖（1 张，含 topic/回帖） |
| batch | 2 | 复用 meeting + 会议任务 |

**共需新增 6 张表。** 这是 schema 变更，属 PRD/SSOT 层面，需产品确认字段口径后才能动。见 Phase 5。

### 两条容易误判的事实

1. **写接口现在就在打真实后端。** `mockRequest` 只包读接口，`create/update/delete/changeStatus` 等全是裸 `request.POST`。所以这些模块的写操作**当前点下去就是 404**，不是"还没切"，是"已切但后端不存在"。因此没有"平滑迁移"，只有"补齐"。
2. **`MOCK_ENABLED` 是全局单布尔**，20 模块共用。任何时刻置 false 都会让所有未接通模块同时打向不存在的接口、满屏报错。requirement 当初是绕开该开关单独直连的。Phase 0 会把它改成按模块粒度，否则后面每接一个模块都要重复这套绕开动作。

---

## 竖切配方（Recipe）

**这是本方案的核心复用单元。** requirement 竖切已产出并验证的 12 类文件，后续每个有表模块按此配方逐一产出。任务描述中「按配方产出」= 严格执行下面 12 步。

以模块 `Xxx`、表 `xxx`、前端 api 目录 `apis/xxx/` 为例：

1. `pojo/XxxDO.java` — **已存在，勿重建**，只核对字段与 schema 一致。
2. `dao/XxxDao.java` + `dao/impl/XxxDaoImpl.java` — `IService<XxxDO>` / `ServiceImpl<XxxMapper, XxxDO>`，`@Repository`。关联表（复合主键）的 Dao 必须写 javadoc 警告：只能 `LambdaQueryWrapper` remove + `saveBatch`，**禁 getById/removeById**。
3. `dto/XxxPageDTO.java` — camelCase 入参，`@Min(1) @Max(500) pageSize`。
4. `dto/XxxCreateDTO.java` — **字段以前端实际 payload 为准**（读 `apis/xxx/types.ts` 的 `XxxCreateParams` 与页面 `doSubmit()` 真实传参），不照抄 PRD。
5. `dto/XxxUpdateDTO.java` — `extends XxxCreateDTO` + `@NotNull Long id` + `@NotNull Integer version`，加 `@EqualsAndHashCode(callSuper = true)`。
6. `vo/XxxPageVO.java` + `vo/XxxDetailVO.java` — 类级 `@JsonNaming(SnakeCaseStrategy)`；时间字段 `@JsonFormat`；DetailVO 带 `version`。**两个 VO 不互相继承**（扁平契约类型，编译期能抓到漂移）。
7. `convert/XxxConvert.java` — 手写，不引 MapStruct。DTO→DO、DO→VO、`applyUpdate`（须 `d.setVersion(dto.getVersion())`，客户端 version 要参与 `WHERE version=?`）、`toIdString(Long)`。
8. `exception/XxxErrorCode.java` — 业务错误码。
9. `service/XxxService.java` + `service/impl/XxxServiceImpl.java` — 写方法开头一律 `currentUser.requireAnyRole(...)`；发布人快照由 `fillPublisher(d, me)` 从**本地 sys_user 单行**取（id/name/departmentId/departmentName），不取 UAA；更新校验 owner，admin 可越权改他人。
10. `controller/XxxController.java` — 4~N 个端点，路径与前端 `url:` 字面量**逐字一致**；**不加** `@PreAuthorize`，并写 javadoc 说明原因。
11. **测试（TDD，先红后绿）**：
    - `convert/XxxConvertTest.java` — 含离线契约断言：`ObjectMapper.writeValueAsString(vo)` 断言 snake_case 键名、日期格式 `yyyy-MM-dd HH:mm:ss`、不出现 camelCase。
    - `service/XxxServiceImplTest.java` — Mockito 单测，覆盖鉴权拒绝/owner 校验/乐观锁冲突。
    - `integration/XxxIntegrationTest.java` — `@Tag("integration") @SpringBootTest`，类名**必须以 Test 结尾**（不是 IT，否则 surefire 不收）。
12. **前端切真**：`apis/xxx/xxxApi.ts` 去掉读接口的 `mockRequest` 包裹（Phase 0 后改为切模块开关）；`xxxAdapter.ts` 的 `total` 改 `Number(data.total ?? 0)`；`types.ts` 补 `version` 字段；表单页 `model.version` 三处联动（声明 / populate 回填 / update 提交）。

**每个模块任务结束时的验收：**
- `mvn test` 全绿（离线）
- `mvn test -Dgroups=integration -Dtest=XxxIntegrationTest` 全绿（真库）
- `pnpm typecheck:all` + self-check PASS
- 浏览器实登录走一遍列表/详情/新建/编辑，确认数据来自 MySQL（用 JDBC 核行）

---

## 阶段划分

| Phase | 内容 | 接口数 | 阻塞 |
|---|---|---|---|
| 0 | 横切地基（模块级 mock 开关、total 批修、公共基类） | 0 | 无 |
| 1 | opportunity（同构验证配方可复制） | 6 | Phase 0 |
| 2 | 简单 CRUD 批：category / auditLog / employee / member | 11 | Phase 1 |
| 3 | 内容运营批：announce / audit | 13 | Phase 2；audit 需补 2 列 |
| 4 | 互动与个人：interaction / notification / profile | 8 | Phase 2 |
| 5 | 聚合统计：home / dashboard / sla | 6 | Phase 3、4（依赖各表有真实数据） |
| 6 | 无表模块：intel / meeting / task / batch / feedback / discussion | 20 | **需产品确认 6 张新表** |

---

# Phase 0 — 横切地基

**为什么必须先做：** 下面三件事每个模块都会踩一次。做一次 vs 做 19 次。

### Task 0.1: mock 开关改为模块粒度

**Files:**
- Modify: `apps/web-app/src/apis/_shared/mock-switch.ts`
- Modify: `apps/web-app/src/apis/requirement/requirementApi.ts:12-24`（删掉绕开开关的注释块，改用新开关）
- Test: `apps/web-app/src/apis/_shared/__tests__/mock-switch.spec.ts`

**Interfaces:**
- Produces: `MOCK_MODULES: Set<string>`、`isMocked(module: string): boolean`、`mockRequest(module, mock, real)`

- [ ] **Step 1: 写失败测试**

```ts
import { describe, it, expect } from 'vitest'
import { isMocked, mockRequest } from '../mock-switch'

describe('mock-switch 模块级开关', () => {
  it('未接通模块仍走 mock', async () => {
    expect(isMocked('intel')).toBe(true)
    const r = await mockRequest('intel', { total: 1 }, () => Promise.reject(new Error('不该走真实')))()
    expect(r).toEqual({ total: 1 })
  })

  it('已接通模块走真实请求', async () => {
    expect(isMocked('requirement')).toBe(false)
    const r = await mockRequest('requirement', { total: 1 }, () => Promise.resolve({ total: 99 }))()
    expect(r).toEqual({ total: 99 })
  })
})
```

- [ ] **Step 2: 跑测试确认 RED**

Run: `pnpm --filter web-app test mock-switch`
Expected: FAIL — `isMocked is not a function`

- [ ] **Step 3: 实现**

```ts
/**
 * 数据来源开关（模块粒度）。
 *
 * 为什么不是全局单布尔：20 个模块的后端是分批接通的。全局 false 会让所有
 * 未接通模块同时打向不存在的接口、满屏 404，淹没当前正在联调模块的信号。
 * 改为白名单后，接通一个模块就从 MOCK_MODULES 里删一个，互不干扰。
 *
 * 维护规则：模块后端联调通过后，删掉这里对应的一行，并在该模块 api 文件
 * 的 mockRequest 调用处保留 module 名不变（调用形态不用改）。
 */
export const MOCK_MODULES = new Set<string>([
  'announce', 'audit', 'auditLog', 'batch', 'category', 'dashboard',
  'discussion', 'feedback', 'home', 'intel', 'interaction', 'meeting',
  'member', 'notification', 'opportunity', 'profile', 'sla', 'task'
])
// 注：'requirement' 已接通，刻意不在表中。

export function isMocked(module: string): boolean {
  return MOCK_MODULES.has(module)
}

/**
 * 包裹真实请求：该模块仍在 mock 名单中则返回本地 DTO 切片，否则执行真实请求。
 * @param module 模块名（须与 apis/ 下目录名一致）
 * @param mock 该接口对应的原始 DTO 切片（与其 adapter 的入参一致）
 * @param real 真实请求 thunk
 */
export function mockRequest(
  module: string,
  mock: unknown,
  real: () => Promise<unknown>
): () => Promise<unknown> {
  return isMocked(module) ? () => Promise.resolve(mock) : real
}
```

- [ ] **Step 4: 跑测试确认 GREEN**

Run: `pnpm --filter web-app test mock-switch`
Expected: PASS (2 passed)

- [ ] **Step 5: 批量补齐 module 参数**

18 个模块的 `mockRequest(mock, real)` 调用改成 `mockRequest('<模块名>', mock, real)`。requirement 恢复用开关：删掉 `requirementApi.ts` 顶部那段"刻意不走 mock-switch"的注释，读接口改回 `mockRequest('requirement', <DTO切片>, () => request.*)`，并恢复 `import mockData from './mocks/requirement.json'`。因 `requirement` 不在名单中，行为不变仍走真实。

- [ ] **Step 6: 门禁**

Run: `pnpm typecheck:all && node <SKILL_DIR>/cli/self-check.mjs`
Expected: 0 error，self-check PASS

- [ ] **Step 7: 人工提交**

建议 message：`refactor(前端): mock 开关改为模块粒度白名单`

---

### Task 0.2: 批修全部 adapter 的 total 类型

**Files:**
- Modify: `apps/web-app/src/apis/*/[a-z]*Adapter.ts`（除 requirement 外全部含分页的 adapter）
- Test: `apps/web-app/src/apis/_shared/__tests__/pagination-total.spec.ts`

**背景：** 后端全局 `Long→String` 序列化会把分页包装类的 `long total` 一并转成字符串（实测 `{"total":"1"}`），而 `PaginationResult.total` 声明是 `number` —— 类型撒谎。脚手架模板一律生成 `total: data.total ?? 0`，**每个模块都有这一行**，切真时逐个中招。

- [ ] **Step 1: 写失败测试**

```ts
import { describe, it, expect } from 'vitest'
import { getOpportunityListAdapter } from '../../opportunity/opportunityAdapter'

describe('分页 total 类型收敛', () => {
  it('后端返回字符串 total 时 adapter 必须转成 number', () => {
    const r = getOpportunityListAdapter({ records: [], total: '42' } as never)
    expect(r.total).toBe(42)
    expect(typeof r.total).toBe('number')
  })
})
```

- [ ] **Step 2: 跑测试确认 RED**

Run: `pnpm --filter web-app test pagination-total`
Expected: FAIL — `expected '42' to be 42`

- [ ] **Step 3: 全量替换**

对每个含分页的 adapter，把
```ts
    total: data.total ?? 0
```
改为
```ts
    // 后端全局 Long→String 序列化会把 total 也转成字符串（实测 "total":"1"），
    // 而 PaginationResult.total 声明是 number。此处强制收敛。
    total: Number(data.total ?? 0)
```

定位命令：`grep -rn "total: data.total" apps/web-app/src/apis/`

- [ ] **Step 4: 跑测试确认 GREEN**

Run: `pnpm --filter web-app test pagination-total`
Expected: PASS

- [ ] **Step 5: 门禁 + 人工提交**

`pnpm typecheck:all` 通过。建议 message：`fix(前端): 收敛全部 adapter 的分页 total 为 number`

---

### Task 0.3: 抽取后端分页与鉴权公共件

**Files:**
- Create: `src/main/java/.../dto/BasePageDTO.java`
- Modify: `src/main/java/.../dto/RequirementPageDTO.java`（改为继承）
- Test: `src/test/java/.../dto/BasePageDTOTest.java`

**Interfaces:**
- Produces: `BasePageDTO { Integer pageNumber; Integer pageSize; String keyword; String sort; }`，供后续 13 个模块的 PageDTO 继承。

- [ ] **Step 1: 写失败测试**

```java
package com.quectel.web.cloud.salesleadhubserver.dto;

import org.junit.jupiter.api.Test;
import javax.validation.Validation;
import javax.validation.Validator;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BasePageDTOTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void defaults_are_page1_size20() {
        BasePageDTO d = new BasePageDTO();
        assertEquals(Integer.valueOf(1), d.getPageNumber());
        assertEquals(Integer.valueOf(20), d.getPageSize());
    }

    @Test
    void rejects_pageSize_over_framework_maxLimit() {
        BasePageDTO d = new BasePageDTO();
        d.setPageSize(501);   // 框架 maxLimit=500，超出会被静默截断
        assertTrue(validator.validate(d).size() > 0, "pageSize>500 必须被校验拦下，不能留给框架静默截断");
    }
}
```

- [ ] **Step 2: 跑测试确认 RED**

Run: `mvn -q test -Dtest=BasePageDTOTest`
Expected: FAIL — cannot find symbol `BasePageDTO`

- [ ] **Step 3: 实现**

```java
package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 分页入参基类。入参保持 camelCase —— 全局 snake_case 是双向的，
 * 设了会让前端 camelCase payload 静默变 null（见序列化契约备忘）。
 */
@Data
public class BasePageDTO {

    /** 页码，从 1 开始 */
    @Min(1)
    private Integer pageNumber = 1;

    /**
     * 每页条数。上限 500 —— 框架 PaginationInnerInterceptor 的 maxLimit=500，
     * 超出不报错而是静默截断，这里显式拦下以免前端拿到少于预期的数据还不知情。
     */
    @Min(1)
    @Max(500)
    private Integer pageSize = 20;

    /** 关键词模糊搜索，可空 */
    private String keyword;

    /** 排序字段，可空 */
    private String sort;
}
```

- [ ] **Step 4: 跑测试确认 GREEN**

Run: `mvn -q test -Dtest=BasePageDTOTest`
Expected: Tests run: 2, Failures: 0

- [ ] **Step 5: RequirementPageDTO 改为继承**

去掉重复的 `pageNumber/pageSize/keyword/sort` 字段，改 `public class RequirementPageDTO extends BasePageDTO`，加 `@EqualsAndHashCode(callSuper = true)`，保留其独有字段。

- [ ] **Step 6: 全量回归**

Run: `mvn test`
Expected: Tests run: 23（原 21 + 新 2），Failures: 0

- [ ] **Step 7: 人工提交**

建议 message：`refactor(后端): 抽取分页入参基类 BasePageDTO`

---

# Phase 1 — opportunity（配方验证）

**为什么选它打头：** 与 requirement 结构最同构（同样是"发布内容 + 分类多对多 + 发布人快照 + 可见性 + 乐观锁"），能最快验证配方是否真的可复制。如果这里顺利，Phase 2 之后可以并行铺开。

### Task 1.1: opportunity 后端竖切

**Files:**
- Create: `dao/OpportunityDao.java`、`dao/impl/OpportunityDaoImpl.java`、`dao/OpportunityCategoryDao.java`、`dao/impl/OpportunityCategoryDaoImpl.java`
- Create: `dto/OpportunityPageDTO.java`、`OpportunityCreateDTO.java`、`OpportunityUpdateDTO.java`
- Create: `vo/OpportunityPageVO.java`、`OpportunityDetailVO.java`
- Create: `convert/OpportunityConvert.java`
- Create: `exception/OpportunityErrorCode.java`
- Create: `service/OpportunityService.java`、`service/impl/OpportunityServiceImpl.java`
- Create: `controller/OpportunityController.java`
- Test: `convert/OpportunityConvertTest.java`、`service/OpportunityServiceImplTest.java`、`integration/OpportunityIntegrationTest.java`

**Interfaces:**
- Consumes: `PageVO<T>`、`CurrentUserResolver.requireAnyRole(...)`、`BasePageDTO`（Task 0.3）
- Produces: 6 个端点，路径逐字对齐前端：`opportunity/page`、`opportunity/detail`、`opportunity/create`、`opportunity/update`、`opportunity/changeStatus`、`opportunity/delete`

**本模块相对配方的 delta（这些是 opportunity 独有的，requirement 没有）：**

1. `status` 是 `draft/published/archived` 三态，不是 requirement 的 SLA 状态机。
2. 多一个 `changeStatus` 端点，语义是**下架/恢复**，schema 注释明确「`archived_by` 谁下架谁恢复」→ 恢复时必须校验 `archived_by == 当前用户 id` 或当前用户是 admin。
3. `attachments` 是 JSON 列 → DO 上需 `@TableField(typeHandler = JacksonTypeHandler.class)` + 类级 `@TableName(autoResultMap = true)`。**核对 `OpportunityDO` 是否已这样标注，没有则补。**
4. 关联表是 `opportunity_category`（复合主键），维护方式同 `request_category`：`LambdaQueryWrapper` 按 `opportunityId` remove + `saveBatch`，禁 `removeById`。
5. `view_count/like_count/collect_count/comment_count` 是计数列，**本任务不实现自增逻辑**（归 Phase 4 的 interaction 模块），page/detail 只读出。
6. `content` 是 `MEDIUMTEXT` 富文本 —— DTO 上不要加 `@Size` 收窄，前端自建 contenteditable 产出的 HTML 可能很长。

- [ ] **Step 1: 读前端真实契约**

Read `apps/web-app/src/apis/opportunity/types.ts` 与发布页的 `doSubmit()`，把 `OpportunityCreateParams` 的**实际字段**抄进 CreateDTO。⛔ 不要照抄 PRD——requirement 当初就踩过（PRD 写 `visibilityScope`，前端实际传 `visibilityType`）。

- [ ] **Step 2: 写 Convert 失败测试**

```java
@Test
void detailVO_serializes_snake_case_and_safe_date() throws Exception {
    OpportunityDO d = new OpportunityDO();
    d.setId(123456789012345678L);
    d.setTitle("5G RedCap 选型方案");
    d.setPublisherDeptName("上海销售组");
    d.setCreateTime(LocalDateTime.of(2026, 7, 1, 9, 12, 0));

    String json = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .writeValueAsString(OpportunityConvert.toDetailVO(d));

    assertTrue(json.contains("\"publisher_dept_name\""), json);
    assertFalse(json.contains("publisherDeptName"), "不得出现 camelCase：" + json);
    // 默认 ISO 的 'T' 会让前端 new Date(s.replace(/-/g,'/')) 解析出 NaN
    assertTrue(json.contains("\"2026-07-01 09:12:00\""), json);
    assertFalse(json.contains("2026-07-01T09:12:00"), "禁 ISO 'T' 分隔：" + json);
}
```

- [ ] **Step 3: 跑测试确认 RED**

Run: `mvn -q test -Dtest=OpportunityConvertTest`
Expected: FAIL — cannot find symbol `OpportunityConvert`

- [ ] **Step 4: 按配方产出 2~10 项文件**

严格执行竖切配方第 2~10 步。

- [ ] **Step 5: 跑测试确认 GREEN**

Run: `mvn -q test -Dtest=OpportunityConvertTest`
Expected: Failures: 0

- [ ] **Step 6: 补 Service 单测（鉴权 + 下架恢复）**

至少覆盖：非 owner 非 admin 改他人 → FORBIDDEN；恢复时 `archived_by` 不匹配且非 admin → FORBIDDEN；陈旧 version 更新 → 冲突错误码。

Run: `mvn -q test -Dtest=OpportunityServiceImplTest`
Expected: Failures: 0

- [ ] **Step 7: 离线全量门禁**

Run: `mvn test`
Expected: 全绿

- [ ] **Step 8: 真库集成测试**

先 `./start-server.ps1` 起服务，再：
Run: `mvn test -Dgroups=integration -Dtest=OpportunityIntegrationTest`
Expected: 全绿。断言至少包含：create 后 `create_by` = 当前 UAA id（10160）而非系统兜底；`opportunity_category` 关联行数正确。

- [ ] **Step 9: 人工提交**

建议 message：`feat(后端): opportunity 模块竖切，对齐 requirement 配方`

### Task 1.2: opportunity 前端切真

- [ ] **Step 1:** `apis/opportunity/types.ts` 补 `version: number`（DTO 侧 `version?: number`）
- [ ] **Step 2:** `opportunityAdapter.ts` 补 `version: dto.version ?? 0`（total 已在 Task 0.2 修完）
- [ ] **Step 3:** 从 `MOCK_MODULES` 删掉 `'opportunity'`
- [ ] **Step 4:** 发布/编辑页 `model.version` 三处联动（声明 / 详情回填 / update 提交），提交外层包 try/catch 保证失败时关掉确认弹窗
- [ ] **Step 5:** 门禁 `pnpm typecheck:all && node <SKILL_DIR>/cli/self-check.mjs` → PASS
- [ ] **Step 6:** 浏览器实登录（atom.ye）走列表→详情→新建→编辑→下架→恢复，用 JDBC 核对 `opportunity` 与 `opportunity_category` 落行
- [ ] **Step 7:** 人工提交，message：`feat(前端): opportunity 模块切真实接口`

---

# Phase 2 — 简单 CRUD 批（11 接口）

这批全是单表、无可见性、无 SLA 的纯 CRUD，是配方最直接的应用。**Phase 1 验证通过后，这 4 个模块可并行**（无相互依赖，文件不重叠）。

### Task 2.1: category（5 接口）

**优先级最高** —— requirement 的分类树当前还依赖 `mocks/requirementOptions.json` 里手写的 id 映射，接通后即可去掉那份手工数据。

- 端点：`category/list`、`category/create`、`category/update`、`category/delete`、`category/changeActive`
- 表：`category`（自关联树，`parent_id`）
- delta：
  - `list` 返回**树形**结构，不是分页 —— 不用 `PageVO`，返回 `List<CategoryTreeVO>`（含 `children`）。
  - 只有 admin 能增删改：`requireAnyRole(ROLE_ADMIN)`。
  - `delete` 必须校验无子节点、且无 `request_category`/`opportunity_category` 引用，否则抛业务错误码而非留下悬空外键。
  - `changeActive` 置 `is_active=0` 时，前端树需过滤 —— 与 requirement 现有过滤逻辑保持一致。
- 收尾：删除 `apis/requirement/mocks/requirementOptions.json` 中手写的 categoryTree，改为调 `category/list`。

### Task 2.2: auditLog（1 接口）

- 端点：`auditLog/page`；表：`audit_log`
- delta：**只读模块**，无写接口。仅 admin 可查。无乐观锁、无 version 字段。

### Task 2.3: employee（1 接口）

- 端点：`employee/page`；表：`sys_user`
- delta：只读。用于人员选择器（可见性配置、任务指派）。**不得返回 email 之外的敏感字段**；返回 `id/name/employeeId/departmentName`。

### Task 2.4: member（4 接口）

- 端点：`member`、`member/page`、`member/add`、`member/update`；表：`product_line_member` + `product_line`
- delta：
  - `is_owner=1` 即 SLA L1 升级人，一条产品线**至多一个 owner** —— add/update 时须校验，否则 Phase 5 的 SLA 升级会取到多人。
  - `user_name` 是快照，从本地 `sys_user` 取，不从 UAA。

**每个任务的步骤序列同配方（读前端契约 → 写 Convert 契约测试 → RED → 产出文件 → GREEN → Service 单测 → `mvn test` → 集成测试 → 前端切真 + 门禁 → 人工提交）。**

---

# Phase 3 — 内容运营批（13 接口）

### Task 3.1: announce（9 接口）

- 端点：前台 `announcement/{page,detail}` + 运营 `operation/announce/{page,detail,create,update,delete,changeStatus,stats}`
- 表：`announcement`
- delta：
  - **前台与运营是两套 VO**：前台只返回已发布且在有效期内的，运营返回全部含草稿。分成 `AnnouncementController` 与 `OperationAnnounceController` 两个类，别挤一个。
  - `operation/announce/stats` 返回聚合计数（各状态条数），不是分页。
  - 运营端全部 `requireAnyRole(ROLE_ADMIN)`；前台端 page/detail 不校角色但要过滤。

### Task 3.2: audit（4 接口）

- 端点：`audit/{page,changeStatus,changePin,delete}`
- 表：`opportunity` + `opportunity_request`（跨两表的运营审核视图）
- ⚠️ **本任务有前置 schema 变更**：`changePin` 需要 `is_pinned`，列表需要 `sort_no`，**两张表当前都没有这两列**（requirement 竖切时已记录为已知缺口）。
- [ ] **Step 0（前置）**：在 `schema.sql` 给 `opportunity` 与 `opportunity_request` 各加 `is_pinned TINYINT(1) NOT NULL DEFAULT 0` 与 `sort_no INT NOT NULL DEFAULT 0`，写好 `ALTER TABLE` 迁移语句并在共享库执行。**这是 schema 变更，执行前需向用户确认。**
- delta：
  - 跨表联合分页：两张表结构不同，**不要写 UNION SQL**。用 `content_type` 参数分流到对应 Dao，前端本来就按 `content_type` 区分（mock 里就有该字段）。
  - 全部端点 `requireAnyRole(ROLE_ADMIN)`。

---

# Phase 4 — 互动与个人（8 接口）

### Task 4.1: interaction（3 接口）

- 端点：`interaction/{comment,comments,like}`；表：`interaction`
- delta（本模块 delta 最多，谨慎）：
  - `reaction_uk` 是**生成列（GENERATED STORED）** → DO 上必须 `@TableField(insertStrategy = NEVER, updateStrategy = NEVER)` 只读映射，否则 insert 会因写只读列失败。
  - like 是**幂等切换**：靠 `uk_inter_reaction` 唯一键，重复点赞捕获 `DuplicateKeyException` 转为"取消点赞"（删行），不是报错。
  - 评论**至多 2 级**：`parent_comment_id` 非空时必须指向一级评论（其自身 `parent_comment_id` 为 null），否则抛业务错误。
  - **软删用 `content_deleted`（普通 TINYINT），不是框架的 `deleted`** —— 后者会隐藏整行，导致子回复一起消失，违反 D7「行保留/内容转占位/留子回复」。
  - comment/like 后需回写目标表的 `comment_count`/`like_count`。**用 `UPDATE ... SET c = c + 1` 原子自增，不要读出来 +1 再写回**（并发会丢计数，且这些列没有乐观锁保护）。

### Task 4.2: notification（4 接口）

- 端点：`notification/{page,markRead,markAllRead,preference/save}`；表：`notification` + `subscription`
- delta：
  - 只能看/改自己的通知：查询强制 `WHERE user_id = 当前用户 id`，`markRead` 须校验归属，否则可以标记别人的通知。
  - `preference/save` 写 `subscription` 表，是全量覆盖语义：按 user_id 删后批插。

### Task 4.3: profile（1 接口）

- 端点：`profile/center`；表：`sys_user` + `interaction` + `subscription` 聚合
- delta：单接口返回 `{user, stats, subscription_tree}` 三段。`stats` 的 6 个计数来自 `interaction` 与内容表的 count 查询。**只返回当前登录人自己的数据**，不接受 userId 入参（避免越权查他人）。

---

# Phase 5 — 聚合统计（6 接口）

**放最后的原因：** 这三个模块产出的是跨表统计数字。在前面各模块尚未写入真实数据时，它们只能返回一堆 0，既无法验证也无法演示。等 Phase 1~4 的表里有真实数据了再做。

### Task 5.1: home（1 接口）
- 端点：`home/dashboard`。返回 `{stats, hot_tags, quick_tasks}`。`quick_tasks` 依赖会议任务表 → **该段先返回空数组**，等 Phase 6 建表后补。

### Task 5.2: dashboard（1 接口）
- 端点：`operation/dashboard`。返回 UV/PV/活跃用户/周发布/响应率/采纳率 + 环比 + 热门内容。
- delta：UV/PV 来自 `view_log`（schema 注释说明 TTL 24h 由应用层按 `viewed_at` 判定）。环比需要上一周期数据 —— **共享库里没有历史数据，首次上线环比只能是 0**，不要为了好看造假数。仅 admin。

### Task 5.3: sla（4 接口）
- 端点：`operation/sla/{page,meta,stats,urge}`；表：`opportunity_request` 的 SLA 字段
- delta：
  - `remaining_text`（"已超时 3时0分"）是**派生展示字段**。在后端算还是前端算需定：建议**后端算**，因为超时判定要用服务器时间，前端时钟不可信。
  - `urge` 催办：L1 升级人来自 `product_line_member.is_owner=1`（依赖 Task 2.4 的唯一 owner 校验）。
  - `escalation_level` 的 L1/L2 跃迁由定时任务驱动还是查询时实时算，需定。建议**查询时实时算**，避免引入调度依赖。

---

# Phase 6 — 无表模块（20 接口）⛔ 需产品决策

**这 6 个模块的数据在 19 张表里一张都没有。** 不是遗漏实现，是从没设计过表。

需新增 6 张表：

| 表 | 服务模块 | 关键字段（据 mock 反推） |
|---|---|---|
| `competitor_intel` | intel | brand/product/intel_type/title/summary/source |
| `industry_intel` | intel | 行业情报，字段待定 |
| `meeting` | meeting、batch | name/meeting_date/recorder_id/recorder_name |
| `meeting_task` | task、meeting、batch、home | meeting_id/task_desc/priority/deadline/assignee_ids/status |
| `feedback` | feedback | title/content/anon_name/emoji/color/like_count |
| `discussion_post` | discussion | title/content/topic + 回帖（可能还需 `discussion_reply`） |

**建议顺序**：`meeting` + `meeting_task` 优先（一次建表解决 meeting/task/batch 三个模块共 9 个接口，且 `home/dashboard` 的 `quick_tasks` 段也依赖它）；`feedback` 次之（单表，3 接口，最简单）；`intel` 与 `discussion` 字段口径最不清晰，放最后。

- [ ] **Step 0（阻塞项）**：与产品确认这 6 张表的字段口径、枚举取值、是否纳入本期。PRD 是 SSOT，**不擅自定义 §10 枚举**。
- [ ] **Step 1**：确认后写 `schema.sql` 增量 + `ALTER`/`CREATE` 迁移脚本，在共享库执行（需用户授权）。
- [ ] **Step 2**：补 `data.sql` 种子（从各模块现有 mock json 转换，保留 dev 固定 id 便于复现）。
- [ ] **Step 3~N**：按竖切配方逐模块产出，顺序 meeting+task+batch → feedback → intel → discussion。

---

## 收尾任务

### Task 7.1: 删除 mock 基础设施

全部模块接通后：
- [ ] `MOCK_MODULES` 应为空集 → 删除 `_shared/mock-switch.ts` 与其测试
- [ ] 各 api 文件去掉 `mockRequest` 包裹与 `import mockData`
- [ ] 删除 `apis/*/mocks/*.json`（21 个文件）
- [ ] `pnpm typecheck:all` + self-check PASS
- [ ] 人工提交：`chore(前端): 移除 mock 数据层，全量接通真实接口`

### Task 7.2: 更新 API 接口文档

- [ ] `docs/sales-lead-hub-API接口文档` 按实际实现回填 68 个端点的最终契约（路径、入参、出参 snake_case 字段、错误码）

---

## 风险与未决项

**必须先决策的（阻塞对应 Phase）：**

1. **Phase 6 的 6 张新表要不要建**（20 接口 = 31% 的工作量）。不建则这些页面永远是 mock 演示态。
2. **Task 3.2 的 `is_pinned` / `sort_no` 两列**要加到 `opportunity` 与 `opportunity_request`，属 schema 变更。
3. **共享库风险**：`192.168.10.28:3306` 上有 359 个库共用 root。所有 `ALTER`/`CREATE` 必须显式 `USE sales_lead_hub_server`，且执行前确认连的是本项目库。

**执行期风险：**

4. **数据量**：`requirement` 的可见性过滤当前是查出后客户端过滤，>500 行会因框架 `maxLimit` 截断而漏数据。opportunity 同构，会继承这个问题。数据量上来前不致命，但要记着。
5. **计数列无乐观锁保护**：`view_count/like_count/comment_count` 并发自增必须用 SQL 原子自增，见 Task 4.1。
6. **仍未提交**：本会话 93 个文件改动尚未 commit。建议在 Phase 0 开始前先分批人工提交现有 requirement 竖切成果，避免后续改动与其纠缠。
