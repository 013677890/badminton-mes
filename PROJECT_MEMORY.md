# Badminton MES 项目进度 Memory

> 更新时间：2026-07-13
>
> 当前工作分支：`PartB`
>
> 记录依据：Git 历史、当前工作区、`badminton-mes.wiki/14-后端模块分工规划.md` 和后端实施文档。

## 1. 项目整体状态

- 后端位于 `backend-java`，技术栈为 Java、Spring Boot、Spring Data JPA、MySQL、Redis、Flyway、Gradle、JUnit 5 和 Mockito。
- 当前已有 `system`、`production`、`equipment`、`barcode` 四个业务包；`scene`、`report`、`quality`、`andon`、`craft`、`wage`、`integration` 等规划模块尚未完整落地。
- 当前主分支为 `main`，B 组开发分支为 `PartB`；本记录创建时 `PartB` 已跟踪 `origin/PartB`。
- 三组职责边界：A 组负责生产订单、工艺、计件工资和 ERP/API；B 组负责条码、现场执行、报工完工、追溯报表、小程序和看板聚合；C 组负责设备、质量和安灯异常闭环。

## 2. 已完成能力

### 公共与系统基础

- 已有 Spring Boot 后端骨架、Gradle 构建、MySQL/Redis 配置、Flyway 迁移和 Docker Compose。
- `system` 已实现认证与权限管理基础能力。
- 已明确 Controller、Service、Repository、DTO/VO、Entity 分层，以及跨模块优先通过 Service/查询接口协作的规则。

### A/C 组已进入当前仓库的能力

- `production` 已有生产工单样例、状态机、物料需求、状态日志及相关 Repository/Service/Controller 基础能力。
- `equipment` 已有设备类别 CRUD、层级校验、循环引用检测和数据库迁移。
- A、C 组的其余规划模块仍需对应责任组继续实现；B 组不得直接访问其他模块的 `dal.repository`。

### B 组 M0：运行、契约与联调基线

- 已完成角色权限、车间/产线/班组/任务数据范围和跨组读写边界整理。
- 已冻结 B 组依赖的 A 组工单、工艺路线、工序、SOP，以及 C 组质量、设备、安灯查询契约。
- Flyway 并行编号规则统一为全仓单调版本 `VyyyyMMddNN`；禁止修改已经进入共享环境的历史迁移。
- 单元测试和外部基础设施集成测试已拆分：默认 `test` 排除 `integration` 标签，`integrationTest` 只运行真实 MySQL/Redis 测试。
- 已补充共享工艺依赖迁移和工艺路线关系 Repository 集成测试，用于验证 B 组创建现场任务前所需的路线、产品、工序和 SOP 关系。

### B 组 M1：`barcode` 条码基础能力

- 已实现条码类型、条码规则及规则明细、条码模板及字段、条码应用规则。
- 已实现条码生成、解析、作废、外部导入、打印记录和使用记录。
- 已实现 Redis 条码流水号、周期重置、容量上限和并发兜底。
- 已提供 Controller、Service、Repository、DTO/VO、Entity、错误码、数据库迁移和单元测试。
- 条码表的 MySQL `unsigned` 类型已通过 JPA `columnDefinition` 对齐，Jackson 引用已适配当前 Spring Boot 依赖版本。

## 3. B 组待完成里程碑

- **M2 `scene` 任务与工序作业：未开始。** 需要实现生产任务单、生产派工单、任务分解、工序开工/暂停/恢复/完成及现场作业查询。
- **M3 报工、完工与返修：未开始。** 需要实现产品生产状态、扫码报工、生产完工、返修工单和关键物料绑定。
- **M4 `report` 追溯与报表：未开始。** 需要实现关键物料追溯、产品追溯、产量、不良、实时生产信息和车间生产时段报表。
- **M5 展示聚合：未开始。** 在 `report` 下实现微信小程序实时看板/生产分析/产品追溯，以及产线、车间、中控电子看板接口。
- 条码模块仍建议补充唯一索引冲突、生成列约束等专项 Repository 集成测试。

## 4. 当前协作约束

- MySQL 是事实数据源；Redis 只用于缓存、流水号、幂等、分布式锁等辅助场景。
- 展示、报表、小程序和看板接口默认只读，不反向修改生产、质量、设备、安灯等核心业务状态。
- B 组跨模块读取 A/C 数据时，应依赖对方 Service 或稳定查询接口，不得直接注入对方 Repository。
- 新增公共能力优先沉淀到 `common`，避免各业务模块重复实现。
- 所有 Flyway 新迁移创建前都要检查全仓最新版本，避免 A/B/C 并行编号冲突。

## 5. 后续接续建议

1. 先以 B 组总体实施规划中的 M2 为下一开发目标，创建 `com.badminton.mes.module.scene`。
2. 在编写 M2 代码前重新核对 A 组 `production`/`craft` 查询契约，避免复制工单或工艺主数据。
3. 按“测试先行、单元测试默认运行、真实基础设施测试打 `integration` 标签”的模式开发。
4. 每完成一个里程碑，同步更新本文件和 `backend-java/docs/B组后端项目总体实施规划.md`。
5. 提交前至少执行 `clean test`；涉及 Flyway、JPA 映射或 Repository 原生 SQL 时，再执行 `integrationTest`。
