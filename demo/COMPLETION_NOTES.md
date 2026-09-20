# OpenSupports 后端交付说明 / Completion Notes

## 交付状态

- `./mvnw clean test` → **BUILD SUCCESS**，51 个 API 集成测试全部通过（0 failures / 0 errors）。
- 应用可独立启动：`./mvnw spring-boot:run`（默认在 `./db.sqlite` 建库并自动执行 `db/schema.sql`）。

## 测试覆盖（BA.md UC-01 ~ UC-18）

| 测试类 | 覆盖用例 |
| --- | --- |
| `BaActorSetupTests` | 验收基线：2 个 user + L1/L2/L3 各 2 个 staff，均可登录 |
| `AccountApiTests` | UC-01 注册、UC-02 登录、UC-03 个人资料 |
| `TicketApiTests` | UC-04 建单、UC-05 工单视图、UC-06 详情、UC-07 回复、UC-08 编辑、UC-09 标签、UC-10 管理、UC-11 高级搜索 |
| `GovernanceApiTests` | UC-12 用户搜索/治理、UC-13 Staff 搜索、UC-14 Staff 管理、UC-15 用户管理 |
| `KnowledgeBaseApiTests` | UC-16 知识库浏览、UC-16b 主题管理、UC-17 文章管理 |
| `DepartmentApiTests` | UC-18 部门管理（CRUD / 迁移 / 默认部门保护 / 权限） |

测试使用真实 HTTP（随机端口）+ SQLite + Spring Session JDBC，通过登录 cookie 模拟不同角色。

## 架构与设计遵循

- 分层：`api/controller` → `application/service` → `Domain/{Model,repo,service}` → `infra/mapper`。
- 聚合根行为内聚在 `Domain/Model`（`Account`、`Ticket`、`Article`、`Folder`），应用服务负责编排与 DTO 包装。
- DTO 与 API 契约遵循 `Analys/Design/API&DTO/设计文档.md`。
- 角色权限：L1/L2 受部门约束，L3=Admin 全局；知识库管理要求 L2+，部门管理要求 L3。

## 与 BA 的有意取舍

1. **邮箱不发送、不验证**：按需求移除所有邮件发送/验证逻辑，邮箱仅作为登录标识；个人资料改邮箱（UC-03）保留。
2. **附件上传**：未实现（`Analys/Design` 已将其排除出首批 MVP）。`attachments` 表与 `Attachment` 模型保留以备后续。
3. **领域事件发布**：聚合内部记录事件，但未接入 AOP 发布器；跨聚合的副作用（如停用员工时解除其工单）由应用服务直接编排完成，保证与 BA 行为一致。

## 运行

```bash
cd demo
./mvnw clean test        # 运行全部 API 集成测试
./mvnw spring-boot:run   # 启动服务（默认 8080）
```
