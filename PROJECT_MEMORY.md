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

## 6. B 组 M2：`scene` 任务、派工与工序作业（2026-07-13）

- 已创建 `com.badminton.mes.module.scene` 业务模块，覆盖 Controller、Service、Entity、Repository、DTO/VO、枚举、错误码和 Redis 编号组件。
- 已实现生产参数及变更日志，包括参数分页、新增/修改和按业务时间查询生效参数。
- 已实现生产任务单创建、修改、提交、审核、下发、暂停、恢复、关闭、取消，以及任务进度查询和操作日志。
- 已实现依据生产任务与工艺路线生成工序派工单和派工明细，并提供派工分页、详情和状态查询。
- 已实现扫码进入工序以及工序开工、暂停、恢复、完工，联动维护任务进度、产品批次当前状态、状态履历和工序履历。
- 已实现车间、产线、班组和任务对象级数据范围校验，写接口从登录上下文获取操作人。
- 已新增 Flyway 迁移 `V2026071301__add_b_group_scene_m2_schema.sql`，用于落地 M2 所需生产参数、任务、派工、批次状态与履历等数据库对象。
- 已新增 M2 聚焦单元测试：生产参数、生产任务、派工、工序作业和数据权限测试，覆盖正常路径、非法状态、越权和关键边界。
- `backend-java/docs/B组后端项目总体实施规划.md` 的 M2 可执行任务已按实际实现标记 `[x]`；文档底部旧的“未开始”摘要仍需在后续规划维护中纠正为真实状态。

## 7. Docker 本地开发环境与旧容器审查（2026-07-13）

- Docker Desktop Linux Engine 已启动并验证可用，Docker Engine 版本为 29.6.1。
- 当前 Compose 只启动了 `mysql` 和 `redis`：`mes-mysql` 使用 MySQL 8.4，绑定 `127.0.0.1:13306`；`mes-redis` 使用 Redis 8.0 Alpine，绑定 `127.0.0.1:16379`；两者均为 `healthy`。
- 本次创建了新的 `badminton-mes_mysql-data` 和 `badminton-mes_redis-data` 数据卷，因此 `.env` 中的 MySQL 首次初始化配置已经实际生效。
- 已使用 `.env` 中的非 root 应用账号完成 MySQL 认证验证，目标数据库为 `badminton_mes`；验证过程未输出密码。Redis 未启用认证，`PING` 返回 `PONG`。
- `.env` 保留 MySQL root 初始化密码、应用账号 `LiuHan` 和应用密码；该文件受 Git 忽略保护，不应提交或在日志中输出秘密值。
- 发现两个昨天创建、目前已停止且不属于当前 Compose 项目的旧独立容器：`badminton-mes-redis`（Redis 7.4，原端口 6380）和 `badminton-mes-mysql`（MySQL 8.4，原端口 3307）。
- 旧 Redis 匿名卷仅约 88 B，基本无业务数据；旧 MySQL 使用独立命名卷 `badminton-mes-mysql-data`，约 235 MB，曾初始化数据库 `badminton-mes`。
- 两个旧容器本身可删除且不会影响当前 `mes-mysql`/`mes-redis`；为防止误删历史数据，建议删除旧容器时不使用 `-v`，并暂时保留旧 MySQL 数据卷，待确认无迁移价值后再单独清理。
- 本轮只检查了旧容器，没有停止或删除任何旧容器、镜像和数据卷。

## 8. B 组 M3 冻结决策（2026-07-13）

- 报工冲销采用非负数量加方向字段：`record_type=1` 正常报工，`record_type=2` 冲销。
- `source_report_id` 唯一，第一阶段仅允许一次全额冲销，不支持部分冲销。
- 完工同步采用 HTTP JSON POST，连接超时 15 秒，整体请求超时 30 秒，最大人工重试 3 次。
- 同步幂等键使用 `FINISH:{finishNo}:{targetSystem}`，默认目标系统为 ERP。
- 第一阶段不实现定时自动重试，只提供明确的人工同步/重试接口。
- 未配置真实同步地址或调用失败时保存失败记录，不得伪造同步成功。

## 9. B 组 M3 当前实现进度（2026-07-13）

- 已新增人工报工和设备计数报工接口，使用 `request_no` 唯一约束和 Service 幂等返回避免重复报工。
- 已校验任务/工序状态、对象级数据权限、投入=良品+不良、返修不超过不良以及任务计划数量上限。
- 已实现一次全额反向冲销，原记录不删除，冲销记录复制数量并通过方向字段形成净额，任务和派工明细汇总同步反向更新。
- 已实现完工单从任务创建、提交审核、审核通过/驳回；只有审核通过状态允许外部同步。
- 已实现可替换 `CompletionSyncClient`：配置 URL 时 HTTP JSON POST 并携带 `Idempotency-Key`；未配置 URL 时显式失败。
- 已保存目标系统、同步状态、重试次数、错误摘要、最后同步时间和稳定幂等键；最多人工尝试 3 次。
- 已新增 `V2026071302__add_b_group_scene_m3_schema.sql` 和独立数据库变更说明，没有修改 Wiki 四个数据库基线文件和历史迁移。
- 已新增报工与完工聚焦单元测试；通过 `Z:\gradlew.bat -p Z:\ clean test --no-daemon` 完成全量单元测试。
- 已使用当前 Docker MySQL 8.4/Redis 运行 `integrationTest --rerun-tasks`，Flyway、Hibernate 映射、Spring 上下文和既有真实 Repository 测试通过。
- 尚未完成的外部项：真实 ERP/WMS 地址端到端演示，以及 A 组工单执行汇总字段的正式写入契约；因此 M3 总里程碑暂不标记为完全完成。

## 10. B 组 M3 扫码报工、完工草稿与专项测试补充（2026-07-13）

- M3 报工已复用 M1 `BarcodeSceneService`：请求传入 `barcodeValue`，服务端校验任务、产品、批次和条码状态，写入 `use_type=3` 的使用记录，并保存可信 `barcodeId`。
- 已接入生产参数 `must_scan_report`：参数为 `1` 时缺少合法条码会拒绝报工；参数为 `0` 时条码可省略，但主动传入仍会校验和留痕。
- 报工工序状态口径已冻结为“作业中或已完成且未暂停”；暂停、待作业和异常工序不能报工。
- 报工事务拆分为外层幂等恢复和内层数据库事务；`request_no` 并发唯一键冲突时失败事务整体回滚，再返回已提交记录，任务和工序汇总只累计一次。
- 完工单新增 `PUT /api/scene/completion_orders/{id}` 修改接口，只允许草稿和审核驳回状态修改完工数量；修改同步更新完工单良品数量，不更新任务汇总，不触发审核或外部同步。
- 完工同步结果已抽为独立事务 Service，原子更新 `prod_finish_order.sync_status` 和 `prod_finish_sync_record`；HTTP 调用继续位于数据库事务之外，失败记录提交后再返回业务错误。
- 已补充 M3 专项数据权限测试：操作工/班组长跨车间、跨产线被拒绝，车间主管可访问本车间其他产线但不可跨车间，越权同步不会调用外部客户端。
- 已补充非法状态、数量边界、扫码必填、条码不匹配、重复请求、重复冲销、完工修改/审核/同步重试等单元测试。
- 已新增真实 MySQL 事务测试，覆盖报工下游失败时条码状态、条码使用记录、报工和汇总全部回滚；相同请求并发提交只形成一条报工；完工审核失败回滚；同步记录和完工单状态原子回滚。
- 验证结果：`compileJava`、ASCII 路径下 `clean test`、连接 Docker MySQL/Redis 的 `integrationTest --rerun-tasks` 全部通过。
- `application-test.yml` 使用 `MES_TEST_DB_*` 和 `MES_TEST_REDIS_*`；从 `.env` 注入测试密码时需要去除外层引号，禁止在日志或文档中输出实际密码。
- M3 仍未整体完成：真实 ERP/WMS 地址端到端演示和 A 组工单汇总写入契约仍待后续完成；报表默认净额/审计发生额展示已在 M4 闭环。

## 11. B 组 M4 追溯、报表与同步导出（2026-07-13）

- 已创建 `com.badminton.mes.module.report`，落地产品/条码追溯、产量报表、实时生产、车间时段、不良聚合和同步导出。
- 产品追溯真实读取当前已落库的 A/B 数据：生产工单、生产任务、条码、扫码使用记录、工序履历、正常/冲销报工和工单物料需求。
- C 组质量、设备、安灯、M5 返修、实际物料消耗批次和装箱明细尚未落库时，响应返回 `dataCompleteness=PARTIAL` 和明确来源警告；禁止伪造空缺业务事实。
- 产量和车间时段报表默认使用报工净额，明细同时保留发生额、冲销额、`recordType`、`sourceReportId` 和冲销原因，M3“报表净额/审计发生额”事项已闭环。
- 不良报表使用 `SCENE_WORK_REPORT`、`QUALITY_INSPECTION`、`REPAIR_RECHECK` 统一来源 DTO；B 组来源已真实接入，C/返修通过可替换适配器降级，聚焦测试验证了跨来源 `defectGroupNo` 去重和无归并号时不自动去重。
- 综合不良率分母冻结为同一查询范围内的报工投入数量净额；分母为 0 时返回 0。
- 同步导出冻结为 UTF-8 CSV、最多 31 天、最多 10000 行；第 10001 行只用于超限判断，超限返回明确业务错误；不建立异步导出队列，不使用 Redis Pub/Sub。
- 查询和导出均由服务端收敛车间、产线范围：管理员可按请求范围查询；非管理员不得扩大所属车间/产线；车间主管可查询本车间其他产线；导出只允许管理员、PMC 和车间主管。
- 已新增 Flyway 迁移 `V2026071303__add_b_group_report_m4_indexes.sql`，只为 `prod_report` 和 `prod_task` 增加报表查询索引，并新增独立数据库变更说明。
- 验证结果：M4 聚焦单元测试在 ASCII 路径通过；Docker MySQL 8.4/Redis `integrationTest --rerun-tasks` 共 10 项、0 失败，覆盖真实 SQL、Flyway、净额、追溯、去重和越权拒绝。
- M4 代码与测试完成；后续进入 M5。M3 仍保留真实 ERP/WMS 端到端演示和 A 组工单汇总写入两个外部依赖事项。
