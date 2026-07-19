# requirement 后端竖切设计（联调试点 · walking skeleton）

- 日期：2026-07-19（v2，经架构师/前端/后端三方评审收口）
- 模块：MOD-02 商机需求 / 后端表 `opportunity_request`（前端模块名 = requirement，落库 = opportunity_request；DO/Mapper 复用**不改名**）
- 定位：本项目**第一条后端业务竖切**，用最小代价建通全链路并验掉框架运行时黑盒，跑通后其余模块**复用"5 黑盒 + 读写分层"结论**（非整模块无脑复制）。
- 决策依据：`PRD/商机平台/决策纪要与修正基线.md`（施工唯一基准）、CLAUDE.md（框架黑盒纪律）、java-coding 详设文档（写码时以其为准）、memory `quectel-mysql-security-backend-conventions`。

## 1. 目标与非目标

**目标**
- 建通 requirement 一个模块后端全链路：`controller → service → service/impl → dao → convert(手写) + dto/vo`。
- 暴露 4 个接口出 Swagger 契约；前端 `apis/requirement/*` 该模块切真联调。
- 验掉框架 **5 大黑盒**：① 审计填充 ② 乐观锁 @Version ③ @PreAuthorize 403 ④ VO 序列化契约 ⑤ JacksonTypeHandler（JSON 快照 List 往返）。

**非目标（本竖切不做）**
- SLA 首响派生（决策 A：不进后端，前端按 `created_at + priority` 现算）。
- delete 接口 / 逻辑删除 @TableLogic 运行时验证（requirement 无 delete，留 opportunity 竖切）。
- `invitedProductLineNames` + `request_product_line` 关联双写（本切只做 `categoryNames` + `request_category` 一路 JSON 快照，边界收敛；产品线邀请留后续）。
- MapStruct（见 §2，首切手写 convert，依赖确认后再切）。
- 其余模块、高级检索、批量。

## 2. 分层架构

```
controller/RequirementController        @RestController + @PreAuthorize；@Valid 校验 + 调 service + 组装 VO
  → service/RequirementService(接口)
     → service/impl/RequirementServiceImpl   业务编排、@Transactional、乐观锁语义、发布人回填、归属校验
        → dao/RequirementDao extends ServiceImpl<OpportunityRequestMapper, OpportunityRequestDO>   IService/lambdaQuery
           → mapper/OpportunityRequestMapper（已存在，复用；勿新建 RequirementMapper）
  ⇄ convert/RequirementConvert            DTO/VO ⇄ DO 手写映射（首切不引 MapStruct）
  dto/  RequirementPageDTO / RequirementCreateDTO / RequirementUpdateDTO   （camelCase，JSR-303）
  vo/   RequirementPageVO / RequirementDetailVO                            （@JsonNaming snake_case）
```

**约定与收口点**（来源：memory 硬约定 + 三方评审）：
- dao 用 `ServiceImpl<OpportunityRequestMapper, OpportunityRequestDO>` + lambdaQuery，不裸写 XML。
- **convert 首切手写**：pom 仅声明了 lombok，未声明 `org.mapstruct:mapstruct` core（父 pom 只在 annotationProcessorPaths 放 processor + binding，注解处理器 ≠ 可 `import org.mapstruct.Mapper`）。引 MapStruct 属 pom 外新依赖，须 §11 审批 + 验证 binding 顺序；为不阻塞首切，先手写 convert 打通，MapStruct 待依赖确认后统一切换。
- 实体 `OpportunityRequestDO`、`OpportunityRequestMapper` 已存在（本会话建），复用不重建、不改名。
- **审计字段禁手动赋值**（框架 `SecurityMetaObjectHandler` 填 create_time/create_by/update_time/update_by）；禁定义 `MyBatisPlusConfig`/`MetaObjectHandler`。
- **发布人快照服务端回填（安全，防伪造）**：`publisherId = SecurityUtils.getCurrentUserId()`，`publisherName/publisherDeptName` 由当前用户/部门快照回填；`RequirementCreateDTO` **不接受**这些字段。
- **归属校验落 service**：update 先 `getById` 加载实体、比对 `publisherId` 判"本人"，@PreAuthorize 只做角色门槛、做不到"本人"。
- **乐观锁走 `dao.updateById(do)`**：update 入参 version 回填到 DO；**按 `updateById` 返回 boolean / 影响行数==0 判冲突**，抛业务错误码（前端提示"数据已被他人修改，请刷新"）；**禁用 `lambdaUpdate().set(...)` 绕过实体 version**（拦截器不介入 → 乐观锁静默失效）。
- **JSON 快照同事务双写**：create/update 在同一 `@Transactional` 内写 `category_names`（`List<String>` + JacksonTypeHandler）+ `request_category` 关联表；detail/page 读回校验 List 往返正确（第 ⑤ 黑盒）。
- DTO 上 JSR-303（`@NotBlank`/`@Size`/枚举合法性），controller `@Valid`；前端为不可信输入。

## 3. 接口契约（对齐前端 `apis/requirement/types.ts`，审后以后端 Swagger 为 SSOT 回流）

| 接口 | 方法 + url | 入参 DTO(camelCase) | 出参 VO(snake_case) | 鉴权 @PreAuthorize |
|---|---|---|---|---|
| 分页 | `POST requirement/page` | RequirementPageDTO(继承框架 `PaginationParams`) | `PaginationResult<RequirementPageVO>` | 登录态 |
| 详情 | `GET requirement/detail?id=` | id:String | RequirementDetailVO | 登录态 |
| 新建 | `POST requirement/create` | RequirementCreateDTO | **id:String** | `hasAnyAuthority('sales','admin')`（表达式口径写码前钉死，见 §5③） |
| 更新 | `POST requirement/update` | RequirementUpdateDTO（**含 version**） | void | 角色门槛 + service 内本人/管理归属校验 |

- 分页入参/出参字段以 `@q-mono-x/types/base` 的 `PaginationParams`/`PaginationResult` **真实结构为准**（写码前 Read 源码；前端 requirementApi 按 `{ records, total }` 用）。
- **状态枚举逐字返回、adapter 不归一**：status ∈ `Pending|Collecting|Adopted|Closed`（**无** published/archived，那是 opportunity/公告的）；urgency ∈ `normal|urgent|critical`；escalationLevel ∈ `L0~L3`。后端必须逐字大小写返回。
- **update 需 version**：前端 `RequirementUpdateParams` 当前**无 version 字段**——②验证前置项 = 前端 types.ts 补 `version` 并从 detail 回填（前端小改，联调前完成）。

## 4. 序列化约定（决策 A 收口版：出参 snake_case 收敛到 VO 级）

- **出参**：业务 VO 用 `@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)`（或 VO 基类统一），只对本模块 VO 生效。前端 requirement adapter 与 mock 已查实为 snake_case，**该模块 adapter 零改动成立**（仅此模块已核，勿当 19 个全量结论写 memory）。
- **入参**：DTO 保持 camelCase（前端送 `categoryIds/visibilityType/pageNumber/pageSize` 均 camelCase）——**不加全局命名策略**，否则入参被反序列化成 snake 找不到键、静默变 null。
- **全局 Jackson 只保留已验证的 Long→String customizer 不动**；`MeController` 的 `Result<User>`、框架分页信封、所有入参**序列化零改动**（保 `MeControllerTest` 基线）。
- MyBatis-Plus 下划线映射（DB 列 ↔ Java 字段）与 Jackson JSON 命名是两层，互不相干：DB 映射走 MP 默认（禁改 MyBatisPlusConfig），JSON 命名纯 Jackson 侧 VO 注解。
- Long 主键经既有 Long→String 序列化为 string，前端无精度丢失。

## 5. 框架 5 大黑盒验证计划

| 黑盒 | 验证方式 | 期望 | 能否离线 |
|---|---|---|---|
| ① 审计填充 | create 后查库；断言前先 `/me` 取当前用户 id 作基准 | `create_time/create_by` 自动填，`create_by`=当前用户 Long | 需真 token（联调） |
| ② 乐观锁 @Version | **确定性陈旧版本重放**：load(v=0)→updateById 成功(→1)→再用手持 v=0 的 DO updateById → 影响 0 行 → 断言冲突抛业务码 | 一成一败，成功方 version+1，冲突显式冒泡 | **不可离线**（拦截器在 SQL 层，须 MySQL 可达） |
| ③ 403 鉴权 | 无权限角色调 create/update；**需 2 个角色 UAA 测试账号**（放行 + 拒绝各一），账号来源联调前确认可得 | 无权限返回 403；写码前钉死 `@PreAuthorize` 表达式（`hasAuthority` vs `hasRole` 前缀、对 DB 角色 sales/product_manager/admin 的映射） | 需真 UAA（联调） |
| ④ VO 序列化 | 前端切真跑 page/detail | snake_case 正确落字段，Long 主键为 string，/me 契约不变 | 联调 |
| ⑤ JacksonTypeHandler | create 写 category_names(List) + request_category → detail 读回 | List 往返一致、关联表同事务落库、detail 展示字段非空 | 需 MySQL（联调/集成测试） |

## 6. 测试策略（CLAUDE.md §9）

- **离线门禁（`mvn test` 必全绿）**：service 层单测（java-coding TDD，Mockito 打桩 dao），只验业务规则（发布人回填、归属校验、冲突分支的抛错）；`MeControllerTest` 基线不破。
- **集成测试（依赖可达才跑，禁进离线门禁）**：② 乐观锁 + ③ 403 + ⑤ JSON 往返用 `@SpringBootTest`。**排除机制**：打 `@Tag("integration")` + surefire `excludedGroups=integration`（或命名 `*IT` 走 failsafe / 单独 profile），否则 `mvn test` 默认扫到、无 MySQL/UAA 假红即破基线。
- service 单测**验不了** ②/③/⑤（乐观锁在 SQL 层、403 在安全过滤链、JSON 在 DB 往返），认清边界，别用 mock 假装验过。

## 7. 联调流程（数据自产自销，无需种子）

1. 起 :8081 + Swagger 出契约；架构师审契约、以 Swagger 为 SSOT，差异回流改前端 `apis/*`。
2. **前端切换范围说明**：`MOCK_ENABLED` 是 `_shared/mock-switch.ts` 的**全局单布尔、无按模块开关**。置 false → **全部模块切真**，仅 requirement 有后端、其余页面预期 404（dev 联调会话可接受）。create/update 本就直连真后端（未包 mockRequest），page/detail 受该布尔控制。若要保其余模块可预览，可后续给 mock-switch 加按模块白名单（非本切必需）。
3. **前端切真前置**：登录态可用（有效 SSO 会话）+ 后端能连 dev 网关 `192.168.10.27:8088`（否则每次真调 401）。**无需动 env/proxy**：`q-cli.config.ts` target `:8081`、rewrite `/api/sales-lead-hub→/sales-lead-hub`、根 `env/.env` base `/sales-lead-hub/` 三处 context-path 已一致。
4. 端到端序列验黑盒：`create ×2`（验①⑤ + 发布人回填）→ `page`（验④）→ `detail`（验④⑤）→ 陈旧版本重放 `update`（验②）→ 无权限角色 `create`（验③）。
5. 全绿则竖切通过；沉淀"框架 5 黑盒已证实（限本切覆盖面）"到 memory，转其余模块。

## 8. 风险与回滚

- **序列化**：VO 级 `@JsonNaming` 若与某框架序列化器叠加异常 → 退每字段 `@JsonProperty`；全局策略已排除（会破 /me + 入参，三方否决）。
- **@EnableGlobalMethodSecurity 未开**（CLAUDE.md 标注未在源码证实）→ 403 验证失败即为发现点，在 security 配置处确认，不在业务侧重复声明。
- **乐观锁拦截器（已取证结案）**：反编译 mysql-starter 的 `MyBatisPlusConfig.mybatisPlusInterceptor()` 确认**只注册 `PaginationInnerInterceptor(MYSQL, maxLimit=500)`，无 `OptimisticLockerInnerInterceptor`** → `@Version` 当前完全失效。解法：业务侧覆盖 `MybatisPlusInterceptor` bean（该 `@Bean` 带 `@ConditionalOnMissingBean`，框架明示允许覆盖），**用户已批准此项 §11 红线破例**，强制要求逐字复刻分页配置。见实施计划 Task 1。
- **MapStruct 依赖缺失**：首切手写 convert 规避；引入前须 §11 审批 + 验 binding 顺序（lombok 先于 mapstruct）。
- **UAA 测试账号**：③ 需两个角色账号，联调前确认可得，否则 403 只能验拒绝侧、验不了放行侧。
- **外推边界**：本切只证 5 黑盒 + 基础读写分层 + 单路 JSON 快照；逻辑删除运行时、interaction 生成列、notification 计数同事务、产品线邀请双写等各模块特性**未覆盖**，外推限定为"5 黑盒 + 读写分层可复用"。
- **回滚**：新增分层文件独立，回滚 = 删新增文件，实体/mapper/DB/前端 types.ts 的 version 小改保留或单独回退。
