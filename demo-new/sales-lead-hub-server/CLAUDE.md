# SalesLeadHubServer — 开发指南

Quectel 微服务项目（销售商机互助平台后端），所有代码必须遵循编码规范。

## 编码规范

编写代码前必读（通过 `curl -s -H "PRIVATE-TOKEN: $GITLAB_TOKEN"` 获取）：

- **Java 编码规范：** `https://git.quectel.com/api/v4/projects/1491/repository/files/quectel-code-java%E5%BC%80%E5%8F%91%E6%96%87%E6%A1%A3%2F%E7%BC%96%E7%A8%8B%E8%A7%84%E8%8C%83.md/raw`

## 项目信息

| 属性 | 值 |
|------|-----|
| artifactId | sales-lead-hub-server |
| 包根路径 | com.quectel.web.cloud.salesleadhubserver |
| 组件 | web, security |
| SSO | quectel-code-security-starter（UAA/OAuth2.0，dev 网关 http://192.168.10.27:8088/api） |

## 包结构

src/main/java/com/quectel/web/cloud/salesleadhubserver/
├── controller/
├── service/impl/
├── dao/
├── mapper/
├── pojo/entity/, dto/, vo/, enums/
└── convert/

## 鉴权与取用户

- 框架已开 `@EnableGlobalMethodSecurity`，业务方法只加 `@PreAuthorize`，禁止重复声明。
- 取当前用户：`com.quectel.code.security.utils.SecurityUtils.getCurrentUser()` → `User`；`getCurrentUserId()` / `hasRole` / `hasAnyRole` / `isAuthenticated`。
