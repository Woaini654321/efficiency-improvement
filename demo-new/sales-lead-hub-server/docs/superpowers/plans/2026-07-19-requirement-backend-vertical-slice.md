# requirement 后端竖切 Implementation Plan（v2，经三方评审收口）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建通 requirement（opportunity_request）后端全链路并出 Swagger，供前端切真联调，验掉框架 5 大运行时黑盒。

**Architecture:** controller→service→impl→dao(ServiceImpl)→convert(手写)+dto/vo；出参 VO `@JsonNaming` snake_case、入参 DTO camelCase 且**按前端实发 payload 对齐**；乐观锁需先覆盖 `MybatisPlusInterceptor` 启用；发布人服务端回填；category 快照 + `request_category` 同事务双写。

**Tech Stack:** Spring Boot 2.7.18 / Java 8(javax) / MyBatis-Plus 3.5.16 / Jackson 2.13.5 / JUnit5 + Mockito 4.5.1 / quectel-code security+mysql+web starter。

## Global Constraints

- 包根 `com.quectel.web.cloud.salesleadhubserver`；分层 controller/service/service.impl/dao/convert/dto/vo（pojo/mapper 已存在）。
- 复用已有 `OpportunityRequestDO` + `OpportunityRequestMapper`（**不新建 RequirementMapper**）；`CategoryDO/CategoryMapper/RequestCategoryDO/RequestCategoryMapper` 也**已存在**，dao 层需新建。
- 审计字段禁手动赋值；**禁定义 `MetaObjectHandler`**。
- **构建命令一律 `mvn`，禁用 `-o`**：实测 `mvn -o` BUILD FAILURE（surefire 2.22.2 的 `_remote.repositories` 标 `central=`，而本项目只声明 `quectel-nexus`，离线拒用）；去掉 `-o` 后 `mvn test` 12s 通过、`Tests run: 3` 全绿。
- **Java 8**：禁 `var`、records、`List.of()`、switch 表达式、文本块。`Arrays.asList` 可用。

### 已反编译取证钉死的框架事实（勿再侦察，直接用）

| 事实 | 取证结论 |
|---|---|
| 全局方法安全 | `SecurityConfig` 上 `@EnableGlobalMethodSecurity(prePostEnabled=true)` **已开**，业务只加 `@PreAuthorize`（CLAUDE.md 存疑点关闭） |
| 权限前缀 | `TokenValidationFilter` 注入 `new SimpleGrantedAuthority("ROLE_" + role)` → **必须用 `hasAnyRole(...)`**，`hasAnyAuthority` 字面比对永不命中、有权限也 403 |
| ErrorCode 枚举 | 只有 `SUCCESS/BAD_REQUEST/PARAM_MISSING/PARAM_INVALID/UNAUTHORIZED/FORBIDDEN/NOT_FOUND/INTERNAL_ERROR/SYSTEM_TIMEOUT/SYSTEM_BUSY`，**无 CONFLICT** |
| Result | 字段 `code/msg/data`；`success()` 无参重载**存在**；成功 code=**0** |
| BaseException | `BaseException(IErrorCode, String)` 双参构造**存在**，`extends RuntimeException` |
| 前端信封 | `@q-cli/libs` 拦截器 `isFailCode = code>299`，成功时 **`return f.data`**（Result 已自动剥离）；错误文案读 **`msg`**。⚠️ 冲突码**禁用 401**，否则前端 `is401` 触发重登录 |
| 校验 | `spring-boot-starter-validation` 由 web-starter 传递**已在位**，包名 `javax.validation.*` |
| 分页 | 框架 `PaginationInnerInterceptor(MYSQL)` + **`setMaxLimit(500L)`**；框架**无**统一分页返回类型 → 自定义 `PageVO<T>` |
| 乐观锁 | 框架 `MyBatisPlusConfig.mybatisPlusInterceptor()` **只注册分页拦截器，无 `OptimisticLockerInnerInterceptor`** → `@Version` 当前完全失效（见 Task 1） |
| 序列化 | web-starter 全局 `serializationInclusion(NON_NULL)` → **null 字段整个 key 从 JSON 消失**（前端须兜底）；web-starter 已有 Long→String，本项目 `JacksonConfig` 的增量实为 `BigInteger` |
| Jackson | 2.13.5；`PropertyNamingStrategy.SnakeCaseStrategy` 已 deprecated → 用 **`PropertyNamingStrategies.SnakeCaseStrategy`**（无 `-Werror`，不用旧的也不会失败，但用新的） |
| 审计填充 | `AutoFillMetaObjectHandler` 带 `@ConditionalOnMissingBean` 退让给 `SecurityMetaObjectHandler`；后者取不到用户时 **fallback 到 system user id** → 黑盒① 须断言 `create_by == /me/id` 而非仅非空 |
| 前端分页入参 | `@q-mono-x/types/base` 的 `PaginationParams = { orderBy?, orderDirection?, pageNumber, pageSize } & T`；requirement 扩展为 `keyword/urgency/status/sort`（**有 sort、无 industry**） |

### 红线破例（用户已明确批准）

- **允许业务侧自定义 `MybatisPlusInterceptor` bean**（Task 1）。依据：框架该 `@Bean` 带 `@ConditionalOnMissingBean`，**设计上明示允许覆盖**；红线本意是防止分页配置丢失，故**强制要求逐字复刻 `PaginationInnerInterceptor(DbType.MYSQL)` + `setMaxLimit(500L)`**。
- 其余红线不变：禁 `MetaObjectHandler`、禁改 env、禁自动 git 写、pom 外新依赖须审批。
- 本计划**不引入任何新依赖**（mockStatic 用 mockito-core 自带的 inline mock maker 资源开关解决）。

### 提交纪律

禁 Claude 自动 git commit（§10）。每个 Task 末尾给中文 Conventional Commit 文案，由**用户手动执行**。

---

### Task 0: 唯一剩余前置（其余侦察项已取证钉死）

- [ ] **Step 1: 索取 2 个 UAA 测试账号**（黑盒③ 唯一外部依赖）
  - 账号甲：含 `sales` 角色 → 期望 `POST /requirement/create` **200**
  - 账号乙：仅 `product_manager` 且非发布人 → 期望 create **403**（角色门槛）、update 他人需求 **403**（service 归属校验）
  - 拿不到则 Task 9 的 ③ 只验拒绝侧，并在验证清单显式标注"放行侧未验"。

- [ ] **Step 2: 确认 pageSize 策略**
  前端 `index.vue`/`form/index.vue` 实发 `{ pageNumber: 1, pageSize: 999 }`，全部筛选在前端内存做；而框架 `maxLimit=500` 会**静默截断**（返 500 条但 total 是全量）。本切数据量小不触发，但须记为已知缺口：数据量 >500 时前端"全量客户端筛选"前提破裂，届时必须把筛选下推后端。DTO 加 `@Max(500)` 显式夹逼，避免静默。

---

### Task 1: 覆盖 MybatisPlusInterceptor 启用乐观锁（红线破例，已获批）

**Files:** Create `.../config/MybatisPlusOptimisticLockConfig.java`

**Interfaces:** Produces：可用的 `@Version` 乐观锁（Task 6/9 依赖）。

- [ ] **Step 1: 写配置（强制复刻分页参数）**
```java
package com.quectel.web.cloud.salesleadhubserver.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 覆盖框架 mysql-starter 的 MybatisPlusInterceptor，以启用乐观锁。
 *
 * <p><b>为何允许覆盖</b>：反编译确认框架 {@code MyBatisPlusConfig.mybatisPlusInterceptor()} 带
 * {@code @ConditionalOnMissingBean}，设计上明示允许业务侧替换；且其实现<b>只注册了分页拦截器</b>，
 * 未注册 {@link OptimisticLockerInnerInterceptor}，导致 {@code @Version} 完全失效。
 * 本项目需乐观锁防止商机需求并发覆盖，经用户明确批准覆盖。</p>
 *
 * <p><b>强制约束</b>：必须逐字复刻框架的分页配置（MYSQL + maxLimit=500），
 * 否则覆盖会连带丢失分页保护。新增拦截器一律追加在分页<b>之前</b>注册，
 * 分页拦截器必须是最后一个（MyBatis-Plus 官方要求）。</p>
 */
@Configuration
public class MybatisPlusOptimisticLockConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 1) 乐观锁（本次新增）——必须在分页之前
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        // 2) 分页——逐字复刻框架原配置，勿改
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(500L);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
```

- [ ] **Step 2: 编译**
Run: `mvn compile` → Expected: BUILD SUCCESS。

- [ ] **Step 3: 启动自证（分页未丢 + 乐观锁已装）**
起服务 `./start-server.ps1`，日志确认无 Bean 冲突；本步的功能验证在 Task 9 集成测试完成（②：陈旧 version 更新影响 0 行；分页：`pageSize=600` 实际返回 ≤500）。
> ⚠️ 若启动报 `MybatisPlusInterceptor` 重复 Bean，说明框架该 Bean 未带 `@ConditionalOnMissingBean`（与取证不符）→ 立即停止，回退删除本文件并向用户报告。

- [ ] **Step 4: 提交文案** `feat:覆盖 MybatisPlusInterceptor 启用乐观锁（复刻分页配置）`

---

### Task 2: 测试基建 + 业务错误码

**Files:**
- Create `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`
- Create `.../exception/RequirementErrorCode.java`

**Interfaces:** Produces：`mockStatic` 可用；`RequirementErrorCode.VERSION_CONFLICT` 供 Task 6。

- [ ] **Step 1: 启用 inline mock maker（零新依赖）**
文件 `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`，内容一行：
```
mock-maker-inline
```
> 依据：`mockito-core 4.5.1` jar 内**自带** `InlineByteBuddyMockMaker`，但默认 MockMaker 是 Subclass 版、不支持静态 mock。此资源文件即官方开关，**不需要 mockito-inline 依赖**（本地 m2 也确实没有它）。

- [ ] **Step 2: 写业务错误码（框架无 CONFLICT）**
```java
package com.quectel.web.cloud.salesleadhubserver.exception;

import com.quectel.code.web.exception.IErrorCode;

/**
 * 本模块业务错误码。框架 ErrorCode 无 CONFLICT 项，故自建。
 *
 * <p>★ javap 已取证 IErrorCode 的契约是 {@code int getCode()} + {@code String getMessageKey()}
 * （i18n key，非字面文案；不是 getMsg）。抛出时用
 * {@code BaseException(IErrorCode, String)} 重载传自定义文案，
 * 该重载会置 {@code isCustomMessage()=true} 从而跳过 MessageSource 解析。</p>
 */
public enum RequirementErrorCode implements IErrorCode {

    /** 乐观锁版本冲突。code 取 409，★ 避开 401（前端 is401 会触发重登录）。 */
    VERSION_CONFLICT(409, "requirement.version.conflict"),
    /** 非本人且非管理员，无权修改 */
    NOT_OWNER(403, "requirement.not.owner");

    private final int code;
    private final String messageKey;

    RequirementErrorCode(int code, String messageKey) { this.code = code; this.messageKey = messageKey; }

    @Override public int getCode() { return code; }
    @Override public String getMessageKey() { return messageKey; }
}
```
> 取证记录（javap `quectel-code-web-starter`）：
> `IErrorCode { int getCode(); String getMessageKey(); }`；
> `BaseException extends RuntimeException`，构造重载有 `(IErrorCode)`、`(IErrorCode, Object[])`、`(IErrorCode, Throwable)`、`(IErrorCode, String)`、`(IErrorCode, String, Throwable)`，另有 `isCustomMessage()`。

- [ ] **Step 3: 编译 + 门禁不回归**
Run: `mvn test` → Expected: BUILD SUCCESS，`Tests run: 3`（MeControllerTest 基线）。

- [ ] **Step 4: 提交文案** `chore:测试基建(inline mock maker) + requirement 业务错误码`

---

### Task 3: DTO 与 VO（按前端实发 payload 对齐）

**Files:** Create `.../dto/RequirementPageDTO.java`、`RequirementCreateDTO.java`、`RequirementUpdateDTO.java`；`.../vo/RequirementPageVO.java`、`RequirementDetailVO.java`、`PageVO.java`

**Interfaces:** Produces：契约类型，供 convert/service/controller 消费。

**⚠️ 契约事实（前端 `form/index.vue` `doPublish()` 第 320–332 行实发）**：
`{ title, urgency, industry, keywords, categoryIds, visibilityType, description }` ——
注意是 **`visibilityType`（非 visibilityScope）**、`categoryIds` 为**字符串** code、`industry` 前端**不校验必填**、`keywords` 后端无列。

- [ ] **Step 1: 写 VO（snake_case + 日期格式 + 补齐 adapter 实读字段）**
```java
package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/** 需求列表项出参。属性 camelCase，经 @JsonNaming 序列化为 snake_case 与前端 adapter 对齐。 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RequirementPageVO {
    private Long requestId;
    private String title;
    private String description;                 // adapter 实读，列表也需要
    private String industry;
    private String urgency;                     // normal/urgent/critical
    private String status;                      // Pending/Collecting/Adopted/Closed
    private String publisherName;
    private String publisherDeptName;
    private List<String> categoryNames;
    private String visibilityType;              // ★ 对应 DO.visibilityScope，JSON 名必须是 visibility_type
    private List<String> visibilityValues;
    private List<String> invitedProductLineNames;
    private String adoptedResponseId;           // string，避免与 Long→String 双重处理歧义
    private String slaStatus;
    private String escalationLevel;             // L0/L1/L2/L3
    private Integer responseCount;
    private Integer viewCount;
    private Integer likeCount;
    private Integer collectCount;
    private Integer commentCount;

    /** ★ 必须带 pattern：默认 ISO 的 'T' 分隔会让前端 replace(/-/g,'/') 解析出 NaN，SLA 倒计时整块不显示 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
}
```
`RequirementDetailVO.java`：字段与上表**完全相同**，另加：
```java
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
    private Integer version;   // 供前端 update 回填（乐观锁）
```
同样 `@Data` + `@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)`。

**本切显式不供的前端字段**（adapter 已有 `?? false`/`?? []` 兜底，不会崩，但功能不可用，须写进 Task 10 验证清单）：
`is_pinned`、`cover_url`（**schema 无此列**，前端 mock 独有）、`invited_product_lines`（含 responded/responder_count，需联查方案表）、`responses`（方案列表，属 solution_response 模块）。

- [ ] **Step 2: 写 PageVO**
```java
package com.quectel.web.cloud.salesleadhubserver.vo;
import lombok.AllArgsConstructor; import lombok.Data;
import java.util.List;
/** 统一分页出参：records + total，与前端 PaginationResult 对齐（框架无分页返回类型）。 */
@Data @AllArgsConstructor
public class PageVO<T> { private List<T> records; private long total; }
```

- [ ] **Step 3: 写 DTO（camelCase，字段名随前端实发）**
```java
package com.quectel.web.cloud.salesleadhubserver.dto;
import lombok.Data;
import javax.validation.constraints.*;
@Data
public class RequirementPageDTO {
    @Min(1) private Integer pageNumber;
    @Min(1) @Max(500) private Integer pageSize;   // 框架 maxLimit=500，显式夹逼避免静默截断
    private String orderBy;
    private String orderDirection;                 // base.ts 有此字段
    private String keyword;
    private String status;
    private String urgency;
    private String sort;                           // 前端扩展字段名是 sort（非 industry）
}
```
```java
package com.quectel.web.cloud.salesleadhubserver.dto;
import lombok.Data;
import javax.validation.constraints.*;
import java.util.List;
/** 新建需求入参。★ 不含 publisherId/publisherName/departmentId/publisherDeptName —— 服务端回填防伪造。 */
@Data
public class RequirementCreateDTO {
    @NotBlank @Size(max = 200) private String title;
    @NotBlank private String description;
    private String industry;                       // ★ 前端不校验必填，故后端不加 @NotBlank
    @NotBlank @Pattern(regexp = "normal|urgent|critical") private String urgency;
    /** ★ 前端实发字段名是 visibilityType（DB 列为 visibility_scope，由 convert 桥接） */
    @NotBlank @Pattern(regexp = "all|dept|personnel") private String visibilityType;
    private List<String> visibilityValues;
    /** ★ 前端 ID 全 string（Long→String 约定），故用 List<String>，service 转 Long */
    private List<String> categoryIds;
}
```
```java
package com.quectel.web.cloud.salesleadhubserver.dto;
import lombok.Data; import lombok.EqualsAndHashCode;
import javax.validation.constraints.NotNull;
/** 更新入参 = 新建入参 + id + version（乐观锁必带）。继承避免字段漂移。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RequirementUpdateDTO extends RequirementCreateDTO {
    @NotNull private Long id;
    @NotNull private Integer version;
}
```

- [ ] **Step 4: 编译**
Run: `mvn compile` → Expected: BUILD SUCCESS。

- [ ] **Step 5: 提交文案** `feat:requirement 竖切 DTO/VO 契约类型（对齐前端实发 payload）`

---

### Task 4: convert 手写 + 序列化离线测试（把黑盒④ 拉进门禁）

**Files:** Create `.../convert/RequirementConvert.java`；Test `src/test/java/.../convert/RequirementConvertTest.java`

**Interfaces:**
- Produces：`toCreateDO(dto)`、`applyUpdate(dto, do)`、`toPageVO(do)`、`toDetailVO(do)`。

- [ ] **Step 1: 写失败测试（映射 + snake_case 序列化）**
```java
package com.quectel.web.cloud.salesleadhubserver.convert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quectel.web.cloud.salesleadhubserver.dto.RequirementCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
import com.quectel.web.cloud.salesleadhubserver.vo.RequirementDetailVO;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class RequirementConvertTest {
    private final RequirementConvert convert = new RequirementConvert();

    @Test
    void toCreateDO_maps_business_fields_but_not_audit_or_publisher() {
        RequirementCreateDTO dto = new RequirementCreateDTO();
        dto.setTitle("5G 模组选型"); dto.setDescription("desc");
        dto.setIndustry("IoT"); dto.setUrgency("urgent"); dto.setVisibilityType("all");
        OpportunityRequestDO d = convert.toCreateDO(dto);
        assertEquals("5G 模组选型", d.getTitle());
        assertEquals("all", d.getVisibilityScope(), "visibilityType -> DO.visibilityScope 桥接");
        assertNull(d.getPublisherId(), "publisherId 必须服务端回填");
        assertNull(d.getCreateBy(), "审计字段禁 convert 赋值");
        assertNull(d.getId(), "id 由雪花生成");
    }

    @Test
    void toDetailVO_exposes_version_and_bridges_visibility() {
        OpportunityRequestDO d = new OpportunityRequestDO();
        d.setId(123L); d.setStatus("Pending"); d.setVersion(2);
        d.setVisibilityScope("dept");
        d.setCategoryNames(Arrays.asList("5G 模组"));
        RequirementDetailVO vo = convert.toDetailVO(d);
        assertEquals(Long.valueOf(123L), vo.getRequestId());
        assertEquals(Integer.valueOf(2), vo.getVersion());
        assertEquals("dept", vo.getVisibilityType());
        assertEquals(Arrays.asList("5G 模组"), vo.getCategoryNames());
    }

    /** 黑盒④ 的契约面：不依赖容器，纯 ObjectMapper 即可断言 snake_case 与日期格式。 */
    @Test
    void detailVO_serializes_to_snake_case_with_plain_datetime() throws Exception {
        OpportunityRequestDO d = new OpportunityRequestDO();
        d.setId(1L); d.setVisibilityScope("all");
        d.setCategoryNames(Arrays.asList("5G 模组"));
        d.setCreateTime(LocalDateTime.of(2026, 7, 1, 9, 12, 0));
        String json = new ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .writeValueAsString(convert.toDetailVO(d));
        assertTrue(json.contains("\"request_id\""), json);
        assertTrue(json.contains("\"category_names\""), json);
        assertTrue(json.contains("\"visibility_type\""), json);
        assertTrue(json.contains("\"2026-07-01 09:12:00\""), "日期须为空格分隔，否则前端解析 NaN: " + json);
        assertFalse(json.contains("\"requestId\""), "不得出现 camelCase: " + json);
    }
}
```

- [ ] **Step 2: 跑测试确认失败**
Run: `mvn test -Dtest=RequirementConvertTest` → Expected: FAIL（类不存在）。

- [ ] **Step 3: 写实现**
```java
package com.quectel.web.cloud.salesleadhubserver.convert;

import com.quectel.web.cloud.salesleadhubserver.dto.*;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
import com.quectel.web.cloud.salesleadhubserver.vo.*;
import org.springframework.stereotype.Component;

/** 手写 DTO/VO ⇄ DO 映射。审计字段与 publisher* 由 service 回填，此处不碰。 */
@Component
public class RequirementConvert {

    public OpportunityRequestDO toCreateDO(RequirementCreateDTO dto) {
        OpportunityRequestDO d = new OpportunityRequestDO();
        d.setTitle(dto.getTitle());
        d.setDescription(dto.getDescription());
        d.setIndustry(dto.getIndustry());
        d.setUrgency(dto.getUrgency());
        d.setVisibilityScope(dto.getVisibilityType());   // 契约名 -> DB 名桥接
        d.setVisibilityValues(dto.getVisibilityValues());
        return d;
    }

    /** 把可改字段与 version 灌入已加载的 DO（乐观锁依赖实体上的 version）。 */
    public void applyUpdate(RequirementUpdateDTO dto, OpportunityRequestDO d) {
        d.setTitle(dto.getTitle());
        d.setDescription(dto.getDescription());
        d.setIndustry(dto.getIndustry());
        d.setUrgency(dto.getUrgency());
        d.setVisibilityScope(dto.getVisibilityType());
        d.setVisibilityValues(dto.getVisibilityValues());
        d.setVersion(dto.getVersion());
    }

    public RequirementPageVO toPageVO(OpportunityRequestDO d) {
        RequirementPageVO v = new RequirementPageVO();
        fillPage(d, v);
        return v;
    }

    public RequirementDetailVO toDetailVO(OpportunityRequestDO d) {
        RequirementDetailVO v = new RequirementDetailVO();
        fillDetail(d, v);
        return v;
    }

    // —— 两个 VO 各自显式赋值（字段集相同，detail 多 updatedAt/version）。刻意不抽公共基类/不用反射：
    //    契约类型保持扁平可读，字段增减时编译器能直接报错。——
    private void fillPage(OpportunityRequestDO d, RequirementPageVO v) {
        v.setRequestId(d.getId());
        v.setTitle(d.getTitle()); v.setDescription(d.getDescription()); v.setIndustry(d.getIndustry());
        v.setUrgency(d.getUrgency()); v.setStatus(d.getStatus());
        v.setPublisherName(d.getPublisherName()); v.setPublisherDeptName(d.getPublisherDeptName());
        v.setCategoryNames(d.getCategoryNames());
        v.setVisibilityType(d.getVisibilityScope());
        v.setVisibilityValues(d.getVisibilityValues());
        v.setInvitedProductLineNames(d.getInvitedProductLineNames());
        v.setAdoptedResponseId(d.getAdoptedResponseId() == null ? null : String.valueOf(d.getAdoptedResponseId()));
        v.setSlaStatus(d.getSlaStatus()); v.setEscalationLevel(d.getEscalationLevel());
        v.setResponseCount(d.getResponseCount()); v.setViewCount(d.getViewCount());
        v.setLikeCount(d.getLikeCount()); v.setCollectCount(d.getCollectCount());
        v.setCommentCount(d.getCommentCount());
        v.setCreatedAt(d.getCreateTime());
    }

    private void fillDetail(OpportunityRequestDO d, RequirementDetailVO v) {
        v.setRequestId(d.getId());
        v.setTitle(d.getTitle()); v.setDescription(d.getDescription()); v.setIndustry(d.getIndustry());
        v.setUrgency(d.getUrgency()); v.setStatus(d.getStatus());
        v.setPublisherName(d.getPublisherName()); v.setPublisherDeptName(d.getPublisherDeptName());
        v.setCategoryNames(d.getCategoryNames());
        v.setVisibilityType(d.getVisibilityScope());
        v.setVisibilityValues(d.getVisibilityValues());
        v.setInvitedProductLineNames(d.getInvitedProductLineNames());
        v.setAdoptedResponseId(d.getAdoptedResponseId() == null ? null : String.valueOf(d.getAdoptedResponseId()));
        v.setSlaStatus(d.getSlaStatus()); v.setEscalationLevel(d.getEscalationLevel());
        v.setResponseCount(d.getResponseCount()); v.setViewCount(d.getViewCount());
        v.setLikeCount(d.getLikeCount()); v.setCollectCount(d.getCollectCount());
        v.setCommentCount(d.getCommentCount());
        v.setCreatedAt(d.getCreateTime());
        v.setUpdatedAt(d.getUpdateTime());
        v.setVersion(d.getVersion());
    }
}
```
> `fillPage`/`fillDetail` 两个私有方法各自显式赋值，不要为了 DRY 抽反射或公共基类。

- [ ] **Step 4: 跑测试确认通过**
Run: `mvn test -Dtest=RequirementConvertTest` → Expected: PASS（3 条）。

- [ ] **Step 5: 提交文案** `feat:requirement 手写 convert + snake_case 序列化门禁测试`

---

### Task 5: dao 层（3 个）

**Files:** Create `.../dao/RequirementDao.java`、`CategoryDao.java`、`RequestCategoryDao.java` + 对应 `.../dao/impl/*Impl.java`

- [ ] **Step 1: 三个接口**
```java
// RequirementDao.java
package com.quectel.web.cloud.salesleadhubserver.dao;
import com.baomidou.mybatisplus.extension.service.IService;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
public interface RequirementDao extends IService<OpportunityRequestDO> { }
```
`CategoryDao extends IService<CategoryDO>`、`RequestCategoryDao extends IService<RequestCategoryDO>` 同构。

- [ ] **Step 2: 三个实现（用 `@Repository`）**
```java
package com.quectel.web.cloud.salesleadhubserver.dao.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.dao.RequirementDao;
import com.quectel.web.cloud.salesleadhubserver.mapper.OpportunityRequestMapper;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
import org.springframework.stereotype.Repository;
@Repository
public class RequirementDaoImpl extends ServiceImpl<OpportunityRequestMapper, OpportunityRequestDO>
        implements RequirementDao { }
```
`CategoryDaoImpl extends ServiceImpl<CategoryMapper, CategoryDO>`、`RequestCategoryDaoImpl extends ServiceImpl<RequestCategoryMapper, RequestCategoryDO>` 同构。
> ⚠️ `RequestCategoryDO` 是复合主键、`requestId` 标 `IdType.INPUT` 仅为让 insert 工作。**关系维护必须走 `LambdaQueryWrapper` 按 requestId 批量 delete + saveBatch，禁用 `selectById`/`removeById`**。

- [ ] **Step 3: 编译** Run: `mvn compile` → BUILD SUCCESS。
- [ ] **Step 4: 提交文案** `feat:requirement 竖切 dao 层`

---

### Task 6: service 核心 CRUD + 单测（发布人回填 / 归属校验 / 乐观锁 / 可见性过滤）

**Files:** Create `.../service/RequirementService.java`、`.../service/impl/RequirementServiceImpl.java`；Test `.../service/RequirementServiceImplTest.java`

**Interfaces:** Produces：`create/update/page/detail` 四方法（签名见下），Task 7 在其 create/update 内补双写，Task 8 消费。

- [ ] **Step 1: 写失败测试**
```java
package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.code.security.utils.SecurityUtils;
import com.quectel.code.web.exception.BaseException;
import com.quectel.web.cloud.salesleadhubserver.convert.RequirementConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.*;
import com.quectel.web.cloud.salesleadhubserver.dto.*;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
import com.quectel.web.cloud.salesleadhubserver.service.impl.RequirementServiceImpl;
import org.junit.jupiter.api.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class RequirementServiceImplTest {
    @Mock RequirementDao dao;
    @Mock CategoryDao categoryDao;
    @Mock RequestCategoryDao requestCategoryDao;
    @Spy  RequirementConvert convert = new RequirementConvert();
    @InjectMocks RequirementServiceImpl service;
    AutoCloseable mocks;

    @BeforeEach void init() { mocks = MockitoAnnotations.openMocks(this); }
    @AfterEach  void close() throws Exception { mocks.close(); }

    private RequirementCreateDTO validCreate() {
        RequirementCreateDTO dto = new RequirementCreateDTO();
        dto.setTitle("t"); dto.setDescription("d"); dto.setIndustry("IoT");
        dto.setUrgency("normal"); dto.setVisibilityType("all");
        return dto;
    }

    @Test
    void create_backfills_publisher_and_initial_status_pending() {
        try (MockedStatic<SecurityUtils> sec = mockStatic(SecurityUtils.class)) {
            sec.when(SecurityUtils::getCurrentUserId).thenReturn(9002L);
            when(dao.save(any())).thenAnswer(inv -> {
                ((OpportunityRequestDO) inv.getArgument(0)).setId(1L); return true; });
            Long id = service.create(validCreate());
            assertEquals(Long.valueOf(1L), id);
            ArgumentCaptor<OpportunityRequestDO> cap = ArgumentCaptor.forClass(OpportunityRequestDO.class);
            verify(dao).save(cap.capture());
            assertEquals(Long.valueOf(9002L), cap.getValue().getPublisherId(), "publisherId 服务端回填");
            assertEquals("Pending", cap.getValue().getStatus(), "初态 Pending");
            assertEquals("L0", cap.getValue().getEscalationLevel());
        }
    }

    @Test
    void update_throws_version_conflict_when_updateById_false() {
        OpportunityRequestDO existing = new OpportunityRequestDO();
        existing.setId(1L); existing.setPublisherId(9002L); existing.setVersion(3);
        when(dao.getById(1L)).thenReturn(existing);
        when(dao.updateById(any())).thenReturn(false);      // 影响 0 行 = 版本冲突
        try (MockedStatic<SecurityUtils> sec = mockStatic(SecurityUtils.class)) {
            sec.when(SecurityUtils::getCurrentUserId).thenReturn(9002L);
            RequirementUpdateDTO dto = new RequirementUpdateDTO();
            dto.setId(1L); dto.setVersion(3);
            dto.setTitle("t2"); dto.setDescription("d"); dto.setUrgency("normal"); dto.setVisibilityType("all");
            BaseException ex = assertThrows(BaseException.class, () -> service.update(dto));
            assertTrue(ex.getMessage() != null && ex.getMessage().contains("已被他人修改"), ex.getMessage());
        }
    }

    @Test
    void update_rejects_non_owner() {
        OpportunityRequestDO existing = new OpportunityRequestDO();
        existing.setId(1L); existing.setPublisherId(9002L); existing.setVersion(1);
        when(dao.getById(1L)).thenReturn(existing);
        try (MockedStatic<SecurityUtils> sec = mockStatic(SecurityUtils.class)) {
            sec.when(SecurityUtils::getCurrentUserId).thenReturn(9999L);   // 非发布人
            RequirementUpdateDTO dto = new RequirementUpdateDTO();
            dto.setId(1L); dto.setVersion(1);
            dto.setTitle("t"); dto.setDescription("d"); dto.setUrgency("normal"); dto.setVisibilityType("all");
            assertThrows(BaseException.class, () -> service.update(dto));
            verify(dao, never()).updateById(any());
        }
    }

    @Test
    void create_rejects_dept_scope_without_values() {
        try (MockedStatic<SecurityUtils> sec = mockStatic(SecurityUtils.class)) {
            sec.when(SecurityUtils::getCurrentUserId).thenReturn(9002L);
            RequirementCreateDTO dto = validCreate();
            dto.setVisibilityType("dept");            // 未给 visibilityValues
            assertThrows(BaseException.class, () -> service.create(dto));
        }
    }

    @Test
    void detail_throws_when_not_found() {
        when(dao.getById(404L)).thenReturn(null);
        assertThrows(BaseException.class, () -> service.detail(404L));
    }
}
```

- [ ] **Step 2: 跑测试确认失败**
Run: `mvn test -Dtest=RequirementServiceImplTest` → Expected: FAIL。

- [ ] **Step 3: 写接口 + 实现**
```java
// RequirementService.java
package com.quectel.web.cloud.salesleadhubserver.service;
import com.quectel.web.cloud.salesleadhubserver.dto.*;
import com.quectel.web.cloud.salesleadhubserver.vo.*;
public interface RequirementService {
    Long create(RequirementCreateDTO dto);
    void update(RequirementUpdateDTO dto);
    PageVO<RequirementPageVO> page(RequirementPageDTO dto);
    RequirementDetailVO detail(Long id);
}
```
```java
package com.quectel.web.cloud.salesleadhubserver.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quectel.code.security.utils.SecurityUtils;
import com.quectel.code.web.exception.BaseException;
import com.quectel.code.web.exception.ErrorCode;
import com.quectel.web.cloud.salesleadhubserver.convert.RequirementConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.*;
import com.quectel.web.cloud.salesleadhubserver.dto.*;
import com.quectel.web.cloud.salesleadhubserver.exception.RequirementErrorCode;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
import com.quectel.web.cloud.salesleadhubserver.service.RequirementService;
import com.quectel.web.cloud.salesleadhubserver.vo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RequirementServiceImpl implements RequirementService {

    private final RequirementDao dao;
    private final CategoryDao categoryDao;
    private final RequestCategoryDao requestCategoryDao;
    private final RequirementConvert convert;

    public RequirementServiceImpl(RequirementDao dao, CategoryDao categoryDao,
                                  RequestCategoryDao requestCategoryDao, RequirementConvert convert) {
        this.dao = dao; this.categoryDao = categoryDao;
        this.requestCategoryDao = requestCategoryDao; this.convert = convert;
    }

    @Override
    @Transactional
    public Long create(RequirementCreateDTO dto) {
        validateVisibility(dto);
        OpportunityRequestDO d = convert.toCreateDO(dto);
        d.setPublisherId(SecurityUtils.getCurrentUserId());     // 服务端回填，防伪造
        // publisherName/departmentId/publisherDeptName 见 Task 6 Step 3b（本地 sys_user 快照）
        d.setStatus("Pending");
        d.setSlaStatus("normal");
        d.setEscalationLevel("L0");
        d.setViewCount(0); d.setResponseCount(0);
        d.setLikeCount(0); d.setCollectCount(0); d.setCommentCount(0);
        d.setCategoryNames(Collections.emptyList());            // Task 7 用真实快照覆盖
        dao.save(d);
        return d.getId();
    }

    @Override
    @Transactional
    public void update(RequirementUpdateDTO dto) {
        validateVisibility(dto);
        OpportunityRequestDO existing = dao.getById(dto.getId());
        if (existing == null) {
            throw new BaseException(ErrorCode.NOT_FOUND, "需求不存在");
        }
        Long uid = SecurityUtils.getCurrentUserId();
        boolean isOwner = existing.getPublisherId() != null && existing.getPublisherId().equals(uid);
        if (!isOwner) {
            throw new BaseException(RequirementErrorCode.NOT_OWNER, "只能修改自己发布的需求");
        }
        convert.applyUpdate(dto, existing);                     // 含 version 回填
        boolean ok = dao.updateById(existing);                  // 乐观锁：WHERE id=? AND version=?
        if (!ok) {
            throw new BaseException(RequirementErrorCode.VERSION_CONFLICT, "数据已被他人修改，请刷新后重试");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageVO<RequirementPageVO> page(RequirementPageDTO dto) {
        int pageNumber = dto.getPageNumber() == null ? 1 : dto.getPageNumber();
        int pageSize   = dto.getPageSize()   == null ? 10 : Math.min(dto.getPageSize(), 500);
        Long uid = SecurityUtils.getCurrentUserId();
        Page<OpportunityRequestDO> p = new Page<>(pageNumber, pageSize);
        IPage<OpportunityRequestDO> r = dao.lambdaQuery()
            .like(dto.getKeyword() != null, OpportunityRequestDO::getTitle, dto.getKeyword())
            .eq(dto.getStatus()  != null, OpportunityRequestDO::getStatus,  dto.getStatus())
            .eq(dto.getUrgency() != null, OpportunityRequestDO::getUrgency, dto.getUrgency())
            // 最小可见性收敛：公开的 或 自己发布的（dept/personnel 精确解析属已知缺口，见 Task 10）
            .and(w -> w.eq(OpportunityRequestDO::getVisibilityScope, "all")
                       .or().eq(OpportunityRequestDO::getPublisherId, uid))
            .orderByDesc(OpportunityRequestDO::getCreateTime)
            .page(p);
        List<RequirementPageVO> records = r.getRecords().stream()
                .map(convert::toPageVO).collect(Collectors.toList());
        return new PageVO<>(records, r.getTotal());
    }

    @Override
    @Transactional(readOnly = true)
    public RequirementDetailVO detail(Long id) {
        OpportunityRequestDO d = dao.getById(id);
        if (d == null) {
            throw new BaseException(ErrorCode.NOT_FOUND, "需求不存在");
        }
        Long uid = SecurityUtils.getCurrentUserId();
        boolean visible = "all".equals(d.getVisibilityScope())
                || (d.getPublisherId() != null && d.getPublisherId().equals(uid));
        if (!visible) {
            throw new BaseException(ErrorCode.FORBIDDEN, "无权查看该需求");
        }
        return convert.toDetailVO(d);
    }

    private void validateVisibility(RequirementCreateDTO dto) {
        if (!"all".equals(dto.getVisibilityType())
                && (dto.getVisibilityValues() == null || dto.getVisibilityValues().isEmpty())) {
            throw new BaseException(ErrorCode.PARAM_INVALID, "选择部门/人员可见时必须指定可见范围");
        }
    }
}
```

- [ ] **Step 3b: 发布人姓名/部门快照**
⚠️ CLAUDE.md §3 明确 **UAA 只给 7 字段、无部门**。故：
- `publisherName` = `SecurityUtils.getCurrentUser().getName()`（UAA 有）；
- `departmentId`/`publisherDeptName` = 查**本地** `sys_user`（新建 `SysUserDao`，backing 已有 `SysUserMapper`）关联 `sys_department` 取快照；
- 本地 `sys_user` 无该 UAA 用户行时**降级**：dept 留 null + `log.warn`，并记入 Task 10 已知缺口（列表卡片部门显示为空）。

- [ ] **Step 4: 跑测试确认通过**
Run: `mvn test -Dtest=RequirementServiceImplTest` → Expected: PASS（5 条）。

- [ ] **Step 5: 全量门禁不回归**
Run: `mvn test` → Expected: BUILD SUCCESS（含 MeControllerTest）。

- [ ] **Step 6: 提交文案** `feat:requirement service 核心 CRUD（回填/归属/乐观锁/可见性）`

---

### Task 7: category 快照 + request_category 同事务双写（黑盒⑤ 业务链路）

**Files:** Modify `.../service/impl/RequirementServiceImpl.java`；Test 追加到 `RequirementServiceImplTest.java`

- [ ] **Step 1: 写失败测试**
```java
    @Test
    void create_writes_category_snapshot_and_join_rows() {
        try (MockedStatic<SecurityUtils> sec = mockStatic(SecurityUtils.class)) {
            sec.when(SecurityUtils::getCurrentUserId).thenReturn(9002L);
            CategoryDO c1 = new CategoryDO(); c1.setId(101L); c1.setName("5G 模组");
            CategoryDO c2 = new CategoryDO(); c2.setId(102L); c2.setName("NB-IoT 模组");
            when(categoryDao.listByIds(anyCollection())).thenReturn(Arrays.asList(c1, c2));
            when(dao.save(any())).thenAnswer(inv -> {
                ((OpportunityRequestDO) inv.getArgument(0)).setId(7L); return true; });
            when(requestCategoryDao.saveBatch(anyCollection())).thenReturn(true);

            RequirementCreateDTO dto = validCreate();
            dto.setCategoryIds(Arrays.asList("101", "102"));
            service.create(dto);

            ArgumentCaptor<OpportunityRequestDO> cap = ArgumentCaptor.forClass(OpportunityRequestDO.class);
            verify(dao).save(cap.capture());
            assertEquals(Arrays.asList("5G 模组", "NB-IoT 模组"), cap.getValue().getCategoryNames(),
                         "categoryNames 快照须落库");
            ArgumentCaptor<java.util.Collection<RequestCategoryDO>> join =
                    ArgumentCaptor.forClass(java.util.Collection.class);
            verify(requestCategoryDao).saveBatch(join.capture());
            assertEquals(2, join.getValue().size(), "request_category 须写 2 行");
        }
    }
```

- [ ] **Step 2: 跑测试确认失败** Run: `mvn test -Dtest=RequirementServiceImplTest` → 新用例 FAIL。

- [ ] **Step 3: 实现双写**

先在 `RequirementServiceImpl` 补齐 import：
```java
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quectel.web.cloud.salesleadhubserver.pojo.CategoryDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.RequestCategoryDO;
```
测试类同样需要 `import com.quectel.web.cloud.salesleadhubserver.pojo.CategoryDO;` 与 `RequestCategoryDO`、`java.util.Arrays`、`static org.mockito.ArgumentMatchers.anyCollection`。

在 `create()` 的 `dao.save(d)` **之前**解析快照、**之后**写关联表：
```java
        List<Long> catIds = parseIds(dto.getCategoryIds());
        List<String> catNames = Collections.emptyList();
        if (!catIds.isEmpty()) {
            catNames = categoryDao.listByIds(catIds).stream()
                    .map(CategoryDO::getName).collect(Collectors.toList());
        }
        d.setCategoryNames(catNames);
        dao.save(d);
        writeJoinRows(d.getId(), catIds);
        return d.getId();
```
```java
    /** 前端 ID 全 string，转 Long；非数字 id 直接判参数非法（避免静默丢分类）。 */
    private List<Long> parseIds(List<String> raw) {
        if (raw == null || raw.isEmpty()) return Collections.emptyList();
        try {
            return raw.stream().map(Long::valueOf).collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new BaseException(ErrorCode.PARAM_INVALID, "分类 id 非法：" + raw);
        }
    }

    /** 关联表复合主键：按 requestId 全删重写，禁 removeById/selectById。 */
    private void writeJoinRows(Long requestId, List<Long> catIds) {
        requestCategoryDao.remove(new LambdaQueryWrapper<RequestCategoryDO>()
                .eq(RequestCategoryDO::getRequestId, requestId));
        if (catIds.isEmpty()) return;
        List<RequestCategoryDO> rows = catIds.stream().map(cid -> {
            RequestCategoryDO r = new RequestCategoryDO();
            r.setRequestId(requestId); r.setCategoryId(cid);
            return r;
        }).collect(Collectors.toList());
        requestCategoryDao.saveBatch(rows);
    }
```
`update()` 在 `dao.updateById` 成功**之后**同样调用：重算 `categoryNames` 并 `writeJoinRows(dto.getId(), catIds)`（同一 `@Transactional` 内）。

- [ ] **Step 4: 跑测试确认通过** Run: `mvn test -Dtest=RequirementServiceImplTest` → PASS（6 条）。
- [ ] **Step 5: 全量门禁** Run: `mvn test` → BUILD SUCCESS。
- [ ] **Step 6: 提交文案** `feat:requirement 分类快照与关联表同事务双写`

---

### Task 8: controller + Swagger

**Files:** Create `.../controller/RequirementController.java`

- [ ] **Step 1: 写 controller（★ 用 hasAnyRole，authority 带 ROLE_ 前缀）**
```java
package com.quectel.web.cloud.salesleadhubserver.controller;

import com.quectel.code.web.pojo.Result;
import com.quectel.web.cloud.salesleadhubserver.dto.*;
import com.quectel.web.cloud.salesleadhubserver.service.RequirementService;
import com.quectel.web.cloud.salesleadhubserver.vo.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

@RestController
@RequestMapping("/requirement")
public class RequirementController {

    private final RequirementService service;
    public RequirementController(RequirementService service) { this.service = service; }

    @PostMapping("/page")
    public Result<PageVO<RequirementPageVO>> page(@RequestBody RequirementPageDTO dto) {
        return Result.success(service.page(dto));
    }

    @GetMapping("/detail")
    public Result<RequirementDetailVO> detail(@RequestParam("id") Long id) {
        return Result.success(service.detail(id));
    }

    /** ★ hasAnyRole 会自动补 ROLE_ 前缀；用 hasAnyAuthority 则永不命中、有权限也 403 */
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('sales','admin')")
    public Result<Long> create(@Valid @RequestBody RequirementCreateDTO dto) {
        return Result.success(service.create(dto));
    }

    @PostMapping("/update")
    @PreAuthorize("hasAnyRole('sales','product_manager','admin')")
    public Result<Void> update(@Valid @RequestBody RequirementUpdateDTO dto) {
        service.update(dto);
        return Result.success();
    }
}
```

- [ ] **Step 2: 全量门禁** Run: `mvn test` → BUILD SUCCESS。
- [ ] **Step 3: 起服务出 Swagger 契约**
`./start-server.ps1` → `http://localhost:8081/sales-lead-hub/swagger-ui/index.html`，确认 4 端点在列。**架构师审契约，此后以 Swagger 为 SSOT**，与前端差异回流（Task 10 Step 1 执行比对）。
- [ ] **Step 4: 提交文案** `feat:requirement controller + 角色鉴权`

---

### Task 9: 集成测试（★ 命名与排除机制已修，避免假绿）

**Files:** Modify `pom.xml`；Create `src/test/java/.../integration/RequirementIntegrationTest.java`

- [ ] **Step 1: pom 属性化排除（★ 硬编码会让 -Dgroups 无法反选）**
```xml
<properties>
  <surefire.excludedGroups>integration</surefire.excludedGroups>
</properties>
...
<plugin>
  <artifactId>maven-surefire-plugin</artifactId>
  <configuration><excludedGroups>${surefire.excludedGroups}</excludedGroups></configuration>
</plugin>
```
Run: `mvn test` → Expected: BUILD SUCCESS，仍 `Tests run: 3+`（现有用例无 `@Tag`，不受排除影响）。

- [ ] **Step 2: 写集成测试（★ 类名必须 `*Test`，`*IT` 不被 surefire 默认扫描）**
```java
package com.quectel.web.cloud.salesleadhubserver.integration;

import com.quectel.web.cloud.salesleadhubserver.dao.RequirementDao;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

/** 需 MySQL 可达。@Tag("integration") + surefire excludedGroups 使其不进离线门禁。 */
@Tag("integration")
@SpringBootTest
class RequirementIntegrationTest {

    @Autowired RequirementDao dao;

    @Test  // 黑盒② 乐观锁 + ⑤ JsonTypeHandler
    void optimistic_lock_stale_replay_and_json_roundtrip() {
        OpportunityRequestDO d = new OpportunityRequestDO();
        d.setTitle("并发测试"); d.setDescription("x"); d.setIndustry("IoT");
        d.setUrgency("normal"); d.setStatus("Pending"); d.setVisibilityScope("all");
        d.setCategoryNames(Arrays.asList("5G 模组", "NB-IoT 模组"));
        dao.save(d);
        Long id = d.getId();
        assertNotNull(id, "雪花 id 应回填");

        OpportunityRequestDO a = dao.getById(id);
        OpportunityRequestDO b = dao.getById(id);          // 两份同 version
        assertEquals(Arrays.asList("5G 模组", "NB-IoT 模组"), a.getCategoryNames(), "⑤ List JSON 往返一致");
        Integer v0 = a.getVersion();
        assertNotNull(v0, "version 应有初值");

        a.setTitle("先改");
        assertTrue(dao.updateById(a), "首次更新应成功");
        assertEquals(Integer.valueOf(v0 + 1), dao.getById(id).getVersion(), "② version 应自增");

        b.setTitle("后改(陈旧 version)");
        assertFalse(dao.updateById(b), "② 陈旧 version 应影响 0 行 → false");
    }
}
```

- [ ] **Step 3: 依赖可达时跑（★ 必须反选排除，并确认 Tests run > 0）**
Run: `mvn test -Dtest=RequirementIntegrationTest -Dgroups=integration -Dsurefire.excludedGroups=`
Expected: **`Tests run: 1, Failures: 0`**。⚠️ **`Tests run: 0` 视为 FAIL**（说明扫描/排除配置仍有误，不是"通过"）。
> 若 `assertFalse(dao.updateById(b))` 失败 → Task 1 的拦截器覆盖未生效，回查 Bean 是否被框架覆盖。

- [ ] **Step 4: 提交文案** `test:requirement 集成测试（乐观锁/JSON 往返）`

---

### Task 10: 前端四处联动 + 契约比对 + 联调验证清单

**Files:** Modify `.../apis/requirement/types.ts`、`requirementAdapter.ts`、`pages/requirement/form/index.vue`；可能 `mocks/requirementOptions.json`

- [ ] **Step 1: Swagger ↔ 前端契约逐字段比对**
把 Task 8 的 Swagger schema 与 `apis/requirement/types.ts` 逐字段比对，差异清单贴回本文件。已知需处理项见 Step 2–4。

- [ ] **Step 2: version 四处联动（只改 types.ts 会让 typecheck 直接红）**
1. `types.ts`：`RequirementDTO` 加 `version?: number`；`RequirementItem` 加 `version: number`；`RequirementUpdateParams` 加 `version: number`。
2. `requirementAdapter.ts` 的 `toItem` 加 `version: dto.version ?? 0`（列表侧落 0 无害）。
3. `form/index.vue`：`FormModel` 加 `version: number`；`populate()` 里 `model.version = d.version`；`doPublish()` 的 update payload 带 `version`。
4. `doPublish()` 加 `try/catch`：冲突时拦截器已 `msg(f.msg)` 弹提示并 reject，不 catch 会冒泡成未处理 promise。

- [ ] **Step 3: 分类 id 对齐（SSOT）**
`requirementOptions.json` 的 cascader value 现为 `"cat1"/"5g"/"gnss"`，**与 `category.json`/DB 的数字 id 不一致**，后端 `parseIds` 会直接判参数非法。改为真实 category id 字符串（`"1"/"101"/"10101"`，见 `db/data.sql`），与 DB 单源对齐。

- [ ] **Step 4: 前端门禁**
Run（`sales-lead-hub-web` 根）：`pnpm typecheck:all`
Run：vue-coding skill 的 `node <SKILL_DIR>/cli/gate-check.mjs --gate=self-check`（或 `pnpm dev:web-app` 触发 predev 自动跑）
Expected: 0 error / PASS。

- [ ] **Step 5: 切真联调**
`_shared/mock-switch.ts` 置 `MOCK_ENABLED = false`。⚠️ 这是**全局单布尔**，其余 30 页会同时切到不存在的后端、满屏 404——属预期，联调期只看 requirement 页。前置：有效 SSO 会话 + 后端能连 `192.168.10.27:8088`。**不动 env/proxy**（context-path 三处已一致）。

- [ ] **Step 6: 5 黑盒人工验证清单**
- [ ] ① 审计填充：create 后查库 `create_by` **等于 `/me/id` 返回值**（不是 fallback 的 system id）、`create_time` 非空
- [ ] ② 乐观锁：两标签页取同 version 先后 update，第二个弹"数据已被他人修改，请刷新后重试"
- [ ] ③ 403：账号乙 create 返回 403；账号甲 create 返回 200
- [ ] ④ 序列化：page/detail 字段为 snake_case、`request_id` 为 string、`created_at` 形如 `2026-07-01 09:12:00`、`/me` 契约不变
- [ ] ⑤ JSON+双写：detail 的 `category_names` 正确回显，且 `request_category` 表行数 == 所选分类数

- [ ] **Step 7: 记录已知缺口（不得静默）**
- `is_pinned`/`cover_url`：schema 无此列，置顶徽标与卡片封面本切不可用
- `invited_product_lines`（邀请回答进度）、`responses`（方案列表）：本切不供，detail 对应区块不显示
- 产品线邀请 `request_product_line` 双写：本切非目标
- `visibilityScope=dept/personnel` 的精确成员解析：本切只做 `all + 本人` 最小收敛
- 本地 `sys_user` 无对应 UAA 用户时，部门快照为空
- 数据量 >500 时前端"全量客户端筛选"前提破裂（maxLimit=500），需把筛选下推后端

- [ ] **Step 8: 沉淀 memory**：把本轮反编译取证（乐观锁拦截器缺失且已覆盖、ROLE_ 前缀、ErrorCode 无 CONFLICT、NON_NULL、mvn -o 不可用）写入 `quectel-mysql-security-backend-conventions`。
- [ ] **Step 9: 提交文案** `feat:requirement 前端 version 联动与分类 id 对齐`

---

## 回滚矩阵

| 改动 | 回滚方式 |
|---|---|
| 新增 Java 文件（config/dto/vo/convert/dao/service/controller/exception/测试） | 直接删除 |
| `pom.xml`（surefire excludedGroups + properties） | 单独 revert 该段 |
| `src/test/resources/mockito-extensions/...` | 删除文件 |
| 前端 `types.ts`/`requirementAdapter.ts`/`form/index.vue`/`requirementOptions.json` | `git checkout HEAD -- <文件>` |
| `MOCK_ENABLED` | 改回 `true` |
| 实体/mapper/DB/schema | **不动**，无需回滚 |

## Self-Review

- **spec 覆盖**：5 黑盒→Task 4(④离线)/9(②⑤)/10(①③及全部人工清单)；序列化收口→Task 3；乐观锁→Task 1+6+9；发布人回填→Task 6 Step3/3b；category 双写→Task 7；@Tag 排除→Task 9 Step1；前端 version→Task 10 Step2；SSOT 回流→Task 8 Step3 + Task 10 Step1。**无遗漏**。
- **占位扫描**：v1 的 `ErrorCode.CONFLICT`/`PREAUTH_EXPR`/`VALID_ON`/`PAGE_WRAP`/CategoryDao「复用或新建」等占位**已全部由反编译取证钉死**并写入 Global Constraints；Task 0 仅剩 UAA 账号（真外部依赖）与 pageSize 策略。Task 6 的 category 相关注释已被 Task 7 的可执行代码取代。
- **类型一致**：`toCreateDO/applyUpdate/toPageVO/toDetailVO` 跨 Task 4/6/7 一致；`PageVO<T>`、`RequirementService` 四方法签名一致；全篇 `OpportunityRequestMapper`；VO 的 `visibilityType` 与 DO 的 `visibilityScope` 桥接点在 convert 单一处，测试已覆盖。
