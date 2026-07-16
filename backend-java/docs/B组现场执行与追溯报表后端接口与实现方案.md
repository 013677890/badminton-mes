# B组现场执行与追溯报表后端接口与实现方案

> 编写日期：2026-07-09  
> 适用范围：`backend-java` 后端工程、B 组负责的 `barcode`、`scene`、`report` 三个业务包，以及微信小程序和电子看板后端聚合接口。

## 1. 阅读结论摘要

当前 `backend-java` 已经完成了一个较完整的 Spring Boot 后端纵向切片：以“生产工单”为样例，贯通了 Controller、VO、Service、Entity、Repository、Redis、Flyway 迁移脚本、统一响应、统一异常、分页模型和测试。它不是空项目，后续 B 组应优先复用现有工程约定，而不是另起一套框架。

B 组“现场执行与追溯报表”的核心工作可以拆成三条主线：

1. `barcode`：条码类型、条码规则、条码模板、条码应用规则、条码生成、扫码解析和追溯入口。
2. `scene`：现场执行，包括参数配置、生产任务单、生产派工单、工序作业、产品生产状态、生产报工、生产完工单、返修工单和平板端接口。
3. `report`：报表分析、产品/物料追溯、微信小程序实时看板、生产分析、产品追溯，以及电子看板的产线、车间、中控聚合接口。

实现上建议先完成“能闭环的最小现场执行链路”：

```text
生产工单 -> 生产任务单 -> 工序派工 -> 条码生成/绑定 -> 工序报工 -> 完工确认 -> 产品追溯/产量报表/看板展示
```

## 2. 当前 `backend-java` 已完成内容

### 2.1 工程与技术栈现状

当前后端工程位于：

```text
backend-java/
```

已确认的工程特征如下：

| 类别 | 当前实现 |
| --- | --- |
| 构建工具 | Gradle，存在 `build.gradle`、`settings.gradle`、Gradle Wrapper |
| Java 版本 | Java 21，`sourceCompatibility`、`targetCompatibility`、`options.release` 均为 21 |
| Spring Boot | `build.gradle` 实际使用 `4.0.7` |
| Web 框架 | `spring-boot-starter-webmvc` |
| ORM | Spring Data JPA / Hibernate |
| 数据库迁移 | Flyway，迁移脚本目录为 `classpath:db/migration` |
| 数据库 | MySQL 8.4 LTS 目标基线，当前 JDBC URL 使用 `jdbc:mariadb://...`，驱动为 MariaDB Connector/J |
| Redis | Spring Data Redis，用于工单缓存和工单号流水 |
| 参数校验 | Jakarta Validation |
| 测试 | Spring Boot Test、WebMVC Test、JUnit Platform |
| Lombok | 已使用，例如 `CommonResult` 使用 `@Data` |

注意：项目级技术栈约束中建议 Spring Boot 使用 `4.1.x`，但当前 `backend-java/build.gradle` 实际是 `4.0.7`。后续如果要统一版本，需要先确认是否升级构建文件。

### 2.2 已搭建的通用框架能力

已具备的通用后端能力如下：

| 能力 | 已有文件/位置 | 说明 |
| --- | --- | --- |
| 应用启动类 | `MesApplication.java` | 根包为 `com.badminton.mes` |
| 统一响应 | `common/core/CommonResult.java` | 响应字段包括 `code`、`message`、`userTip`、`data` |
| 错误码基础模型 | `common/core/ErrorCode.java`、`GlobalErrorCodeConstants.java` | 成功码固定为 `00000` |
| 业务异常 | `common/exception/ServiceException.java` | Service 层抛出业务异常 |
| 全局异常处理 | `common/exception/GlobalExceptionHandler.java` | 统一将异常转为 `CommonResult` |
| 分页模型 | `common/core/PageParam.java`、`PageResult.java` | 支持分页请求和分页结果 |
| 通用状态枚举 | `common/enums/CommonStatusEnum.java` | 启用/停用等基础状态 |
| Flyway 管库 | `resources/db/migration/V1__init_production_sample.sql` | JPA 不自动建表，`ddl-auto: none` |
| Redis 封装样例 | `module/production/dal/redis/*` | 已有 Key 常量、缓存 DTO、工单号流水生成 |

### 2.3 已完成的业务切片：生产工单

当前最完整的业务模块是：

```text
com.badminton.mes.module.production
```

已包含的分层结构：

```text
module/production/
  constants/
  controller/
    vo/
  convert/
  dal/
    entity/
    redis/
    repository/
  enums/
  service/
    impl/
```

生产工单已具备的接口能力：

| 接口能力 | 路径风格 | 说明 |
| --- | --- | --- |
| 创建工单 | `POST /api/production/work_orders` | 创建生产工单，支持系统生成工单号 |
| 修改工单 | `PUT /api/production/work_orders/{id}` | 仅“已创建”状态允许修改计划信息 |
| 删除工单 | `DELETE /api/production/work_orders/{id}` | 逻辑删除，仅“已创建”状态允许删除 |
| 下达工单 | `PUT /api/production/work_orders/{id}/release` | 状态从“已创建”变为“已下达” |
| 查询详情 | `GET /api/production/work_orders/{id}` | 带 Redis 缓存 |
| 分页查询 | `GET /api/production/work_orders/page` | 支持分页和条件筛选 |

生产工单 Service 中已经体现的实现约定：

- Controller 只做参数校验、调用 Service 和统一响应包装。
- Service 负责业务校验、事务边界、数据库写入和缓存失效。
- 写接口使用 `@Transactional(rollbackFor = Exception.class)`。
- 查询接口使用 `@Transactional(readOnly = true)`。
- 通过数据库唯一约束兜底并发重复写入。
- 修改、删除、下达等状态流转使用 CAS 风格更新，降低并发竞态风险。
- 先写数据库，写成功后删除缓存，短暂不一致由 TTL 兜底。
- 使用 `WorkOrderConvert` 进行 Entity/VO/DTO 转换。

### 2.4 当前数据库样例切片

`V1__init_production_sample.sql` 已创建三张表：

| 表名 | 说明 | 与 B 组关系 |
| --- | --- | --- |
| `base_workshop` | 车间表 | B 组现场任务、报工、报表均会按车间过滤 |
| `base_product` | 产品表 | B 组条码、任务、追溯和报表都需要产品信息 |
| `prod_work_order` | 生产工单主表 | B 组现场执行的上游来源 |

`prod_work_order` 已包含 B 组后续可直接复用的字段：

- `work_order_no`：工单号，可作为制令单号追溯入口。
- `product_id`、`product_name`、`spec`：产品基础信息。
- `batch_no`：生产批次号，已有索引 `idx_batch_no`，适合产品追溯反查。
- `workshop_id`：车间维度。
- `plan_quantity`：计划数量。
- `dispatched_quantity`：已派工数量。
- `input_quantity`：投入数量汇总。
- `finish_quantity`：完工数量汇总。
- `defect_quantity`：不良数量汇总。
- `rework_quantity`：返修数量汇总。
- `order_status`：工单状态。
- `kit_status`：齐套状态。

### 2.5 B 组可复用的现有约定

B 组新增模块时建议直接沿用以下约定：

| 类型 | 约定 |
| --- | --- |
| 包结构 | `controller`、`controller/vo`、`service`、`service/impl`、`dal/entity`、`dal/repository`、`dal/redis`、`convert`、`constants`、`enums` |
| 请求对象 | `XxxSaveReqVO`、`XxxPageReqVO`、动作类用 `XxxSubmitReqVO`、`XxxAuditReqVO` |
| 响应对象 | `XxxRespVO` |
| 分页结果 | `CommonResult<PageResult<XxxRespVO>>` |
| 普通结果 | `CommonResult<T>` |
| 业务异常 | `throw new ServiceException(ModuleErrorCodeConstants.Xxx)` |
| 错误码 | 每个模块建立自己的 `XxxErrorCodeConstants` |
| 数据访问 | Spring Data JPA `Repository` + 必要的 `Specification` |
| 数据库变更 | Flyway 新增 `V2__xxx.sql`、`V3__xxx.sql` 等脚本 |
| Redis | Key 常量集中在各模块 `dal.redis` 下，不在 Service 中散落硬编码 |

## 3. B 组业务边界

### 3.1 B 组负责模块

根据 `14-后端模块分工规划.md`，B 组负责：

| 模块 | 逻辑归属 | 说明 |
| --- | --- | --- |
| 条码应用 | `barcode` | 条码类型、规则、模板、应用规则、条码生成和解析 |
| 现场管理 | `scene` | 现场执行过程数据，是报表和追溯的主要数据来源 |
| 报表分析 | `report` | 产量、不良、实时信息、时段报表、追溯查询 |
| 微信小程序后端 | `report` 展示聚合 | 不单独建主数据模块，作为移动端聚合接口 |
| 电子看板后端 | `report` 展示聚合 | 不单独建主数据模块，作为大屏聚合接口 |

推荐包名：

```text
com.badminton.mes.module.barcode
com.badminton.mes.module.scene
com.badminton.mes.module.report
```

### 3.2 B 组核心数据流

建议按照以下主链路落地：

```text
A组生产工单/工艺路线
  -> B组生产任务单
  -> B组工序派工单
  -> B组条码生成/批次绑定
  -> B组工序作业/生产报工
  -> B组产品生产状态
  -> B组生产完工单/返修工单
  -> B组产品追溯/产量报表/小程序/电子看板
```

### 3.3 与 A/C 组协作边界

| 协作对象 | B 组需要读取的数据 | 当前推荐方式 |
| --- | --- | --- |
| A 组 `production` | 工单、产品、车间、计划数量、工单状态、批次、汇总字段 | 允许 B 组直接查询生产工单及相关基础表；写入时仅更新双方约定的执行汇总字段 |
| A 组 `craft` | 工艺路线、工序、SOP 绑定 | 允许 B 组在生成派工和展示 SOP 时直接查询工艺相关表 |
| A 组 `wage` | 计件工资可能反向读取 B 组报工结果 | 计件工资可直接读取 B 组报工事实表，按报工正向/反向净额统计 |
| C 组 `quality` | 检验结果、不良原因、质量放行状态 | 允许 B 组报表和追溯直接查询质量结果表，但不修改质量核心状态 |
| C 组 `equipment` | 设备状态、设备计数、OEE、停机时长 | 允许 B 组看板和报表直接查询设备状态/OEE 表，但不维护设备台账 |
| C 组 `andon` | 现场异常、安灯处理状态、停线影响 | 允许 B 组看板、追溯和时段报表直接查询安灯事件表，但不关闭或处理安灯异常 |

边界原则：

- 当前课程项目允许 B 组直接查询 A/C 组相关业务表，以降低实现复杂度。
- 直接查表时必须遵守统一登录权限模块提供的数据权限条件。
- A/C 被 B 依赖的字段、枚举和表结构变更前需要提前同步。
- `report`、小程序、电子看板接口默认只读，不反向修改生产、质量、设备、安灯等核心业务状态。
- Redis 只作为缓存、编号、幂等、锁、看板快照等辅助能力，MySQL 是事实数据源。

## 4. 后端技术栈与工程约定

### 4.1 技术栈

B 组应使用现有 `backend-java` 技术栈：

| 技术 | 用法 |
| --- | --- |
| Java 21 | 后端统一语言级别 |
| Spring Boot 4.x | Web、Validation、JPA、Redis、Flyway、Test 基础框架 |
| Spring WebMVC | REST API |
| Jakarta Validation | 请求参数校验 |
| Spring Data JPA | 业务表 CRUD、分页、动态查询 |
| Flyway | 数据库表结构和种子数据迁移 |
| MySQL 8.4 LTS | 事实数据源 |
| Redis | 条码流水、报表快照、热点详情缓存、接口幂等、短锁 |
| Lombok | 简化 VO/DTO/Entity 样板代码，但核心逻辑仍保持清晰 |
| JUnit + Spring Boot Test | Controller、Service、Redis、Repository 测试 |

### 4.2 API 通用约定

建议 B 组接口统一使用以下风格：

```text
/api/barcode/...
/api/scene/...
/api/report/...
```

约定如下：

- 路径使用小写字母和下划线，例如 `work_reports`、`production_tasks`。
- 资源名优先使用复数名词。
- 创建使用 `POST`。
- 修改使用 `PUT`。
- 删除使用 `DELETE`，默认逻辑删除。
- 查询详情使用 `GET /{id}`。
- 分页查询使用 `GET /page`。
- 动作类接口采用动作子路径，例如 `/{id}/audit`、`/{id}/start`、`/{id}/pause`。
- 所有接口返回 `CommonResult<T>`。
- 分页列表返回 `CommonResult<PageResult<XxxRespVO>>`。
- 请求体使用 `@Valid @RequestBody`。
- 路径参数使用 `@Positive` 等约束。

### 4.3 模块目录建议

每个 B 组模块建议按当前 `production` 模块风格组织：

```text
com.badminton.mes.module.barcode
  constants
  controller
    vo
  convert
  dal
    entity
    redis
    repository
  enums
  service
    impl

com.badminton.mes.module.scene
  constants
  controller
    vo
  convert
  dal
    entity
    redis
    repository
  enums
  service
    impl

com.badminton.mes.module.report
  constants
  controller
    vo
  convert
  dal
    entity
    redis
    repository
  service
    impl
```

## 5. `barcode` 条码应用接口规划

### 5.1 条码类型接口

Controller 建议：

```text
BarcodeTypeController
```

基础路径：

```text
/api/barcode/types
```

| 方法 | 路径 | 说明 | 写/读 |
| --- | --- | --- | --- |
| `POST` | `/api/barcode/types` | 新增条码类型 | 写 |
| `PUT` | `/api/barcode/types/{id}` | 修改条码类型 | 写 |
| `PUT` | `/api/barcode/types/{id}/enable` | 启用条码类型 | 写 |
| `PUT` | `/api/barcode/types/{id}/disable` | 停用条码类型 | 写 |
| `DELETE` | `/api/barcode/types/{id}` | 删除条码类型，已被规则使用时禁止删除 | 写 |
| `GET` | `/api/barcode/types/{id}` | 查询条码类型详情 | 读 |
| `GET` | `/api/barcode/types/page` | 分页查询条码类型 | 读 |
| `GET` | `/api/barcode/types/options` | 查询启用状态的条码类型选项 | 读 |

核心实现：

- 编码唯一性通过应用层校验和数据库唯一索引共同保证。
- 删除前检查是否被 `barcode_rule` 或 `barcode_application_rule` 使用。
- 停用后不再出现在规则配置选项中。

### 5.2 条码规则接口

Controller 建议：

```text
BarcodeRuleController
```

基础路径：

```text
/api/barcode/rules
```

| 方法 | 路径 | 说明 | 写/读 |
| --- | --- | --- | --- |
| `POST` | `/api/barcode/rules` | 新增条码规则 | 写 |
| `PUT` | `/api/barcode/rules/{id}` | 修改条码规则，只影响新生成条码 | 写 |
| `PUT` | `/api/barcode/rules/{id}/enable` | 启用规则 | 写 |
| `PUT` | `/api/barcode/rules/{id}/disable` | 停用规则 | 写 |
| `DELETE` | `/api/barcode/rules/{id}` | 删除未被使用的规则 | 写 |
| `GET` | `/api/barcode/rules/{id}` | 查询规则详情，含组成项 | 读 |
| `GET` | `/api/barcode/rules/page` | 分页查询规则 | 读 |
| `POST` | `/api/barcode/rules/preview` | 按规则配置预览生成效果 | 读/计算 |
| `POST` | `/api/barcode/rules/validate` | 校验规则配置是否合法 | 读/计算 |

核心实现：

- 规则由常量、日期、产品编码、产线编码、批次流水号等组成项拼接。
- 规则明细单独建表，避免把复杂 JSON 作为唯一事实数据源。
- 规则生成结果必须唯一，数据库对条码值建立唯一索引兜底。
- 流水号建议用 Redis `INCR` 生成，Key 包含规则、日期周期和业务维度。
- Redis 生成后仍要落库到 `barcode_instance`，MySQL 是事实数据源。

### 5.3 条码模板接口

Controller 建议：

```text
BarcodeTemplateController
```

基础路径：

```text
/api/barcode/templates
```

| 方法 | 路径 | 说明 | 写/读 |
| --- | --- | --- | --- |
| `POST` | `/api/barcode/templates` | 新增标签模板 | 写 |
| `PUT` | `/api/barcode/templates/{id}` | 修改标签模板，已绑定时形成新版本 | 写 |
| `PUT` | `/api/barcode/templates/{id}/enable` | 启用模板 | 写 |
| `PUT` | `/api/barcode/templates/{id}/disable` | 停用模板 | 写 |
| `GET` | `/api/barcode/templates/{id}` | 查询模板详情 | 读 |
| `GET` | `/api/barcode/templates/page` | 分页查询模板 | 读 |
| `POST` | `/api/barcode/templates/preview` | 预览标签打印内容 | 读/计算 |

核心实现：

- 模板必须包含条码值或二维码值字段。
- 模板字段建议独立建表，支持字段位置、字体、数据来源等配置。
- 已绑定应用规则的模板修改时保留版本记录。
- 第一阶段可只输出预览数据结构，不直接对接真实打印机。

### 5.4 条码应用规则接口

Controller 建议：

```text
BarcodeApplicationRuleController
```

基础路径：

```text
/api/barcode/application_rules
```

| 方法 | 路径 | 说明 | 写/读 |
| --- | --- | --- | --- |
| `POST` | `/api/barcode/application_rules` | 新增条码应用规则 | 写 |
| `PUT` | `/api/barcode/application_rules/{id}` | 修改应用规则 | 写 |
| `PUT` | `/api/barcode/application_rules/{id}/enable` | 启用应用规则 | 写 |
| `PUT` | `/api/barcode/application_rules/{id}/disable` | 停用应用规则 | 写 |
| `DELETE` | `/api/barcode/application_rules/{id}` | 删除未被业务使用的规则 | 写 |
| `GET` | `/api/barcode/application_rules/{id}` | 查询规则详情 | 读 |
| `GET` | `/api/barcode/application_rules/page` | 分页查询应用规则 | 读 |
| `GET` | `/api/barcode/application_rules/options` | 查询生成条码时可用规则 | 读 |

核心实现：

- 同一产品/物料、同一条码类型只允许一条启用默认规则。
- 启用前检查条码类型、条码规则和模板均为启用状态。
- 支持条码模式：唯一码、批次码。
- 支持条码来源：规则生成、传入值生成、外部导入。

### 5.5 条码生成、打印、解析接口

Controller 建议：

```text
BarcodeGenerationController
```

基础路径：

```text
/api/barcode/instances
```

| 方法 | 路径 | 说明 | 写/读 |
| --- | --- | --- | --- |
| `POST` | `/api/barcode/instances/generate` | 根据应用规则生成单个或少量条码 | 写 |
| `POST` | `/api/barcode/instances/batch_generate` | 批量生成条码 | 写 |
| `POST` | `/api/barcode/instances/import` | 外部导入条码 | 写 |
| `POST` | `/api/barcode/instances/{id}/print` | 打印或记录打印动作 | 写 |
| `PUT` | `/api/barcode/instances/{id}/cancel` | 作废未使用条码 | 写 |
| `GET` | `/api/barcode/instances/{id}` | 查询条码详情 | 读 |
| `GET` | `/api/barcode/instances/page` | 查询条码生成记录 | 读 |
| `POST` | `/api/barcode/instances/parse` | 解析条码值，返回业务对象上下文 | 读 |
| `GET` | `/api/barcode/instances/{id}/use_records` | 查询扫码使用记录 | 读 |

核心实现：

- `barcode_value` 必须唯一，数据库唯一索引兜底。
- 批量生成时使用 Redis 流水减少锁竞争，但最终以 MySQL 落库结果为准。
- 外部导入要校验格式、重复性和应用规则适配性。
- 已使用条码不能作废。
- 重复打印要记录打印次数和原因。
- 扫码使用记录要关联任务、工序、人员、设备和业务时间。

## 6. `scene` 现场管理接口规划

### 6.1 参数配置接口

Controller 建议：

```text
SceneProductionParameterController
```

基础路径：

```text
/api/scene/production_parameters
```

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/scene/production_parameters` | 新增参数 |
| `PUT` | `/api/scene/production_parameters/{id}` | 修改参数，关键参数记录变更原因 |
| `PUT` | `/api/scene/production_parameters/{id}/enable` | 启用参数 |
| `PUT` | `/api/scene/production_parameters/{id}/disable` | 停用参数 |
| `GET` | `/api/scene/production_parameters/{id}` | 查询参数详情 |
| `GET` | `/api/scene/production_parameters/page` | 分页查询参数 |
| `GET` | `/api/scene/production_parameters/effective` | 查询某车间/产线/产品当前生效参数 |
| `GET` | `/api/scene/production_parameters/{id}/change_logs` | 查询参数变更日志 |

需要支持的关键参数包括：

- 是否允许超产。
- 是否必须扫码报工。
- 是否启用首件检验。
- 是否允许跳工序。
- 是否启用安灯联动。

### 6.2 生产任务单接口

Controller 建议：

```text
SceneProductionTaskController
```

基础路径：

```text
/api/scene/production_tasks
```

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/scene/production_tasks` | 创建生产任务单 |
| `PUT` | `/api/scene/production_tasks/{id}` | 修改未审核任务 |
| `PUT` | `/api/scene/production_tasks/{id}/audit` | 审核任务 |
| `PUT` | `/api/scene/production_tasks/{id}/release` | 下发任务到现场 |
| `PUT` | `/api/scene/production_tasks/{id}/start` | 开工 |
| `PUT` | `/api/scene/production_tasks/{id}/pause` | 暂停，需记录原因 |
| `PUT` | `/api/scene/production_tasks/{id}/resume` | 恢复 |
| `PUT` | `/api/scene/production_tasks/{id}/close` | 关闭 |
| `GET` | `/api/scene/production_tasks/{id}` | 任务详情 |
| `GET` | `/api/scene/production_tasks/page` | 分页查询任务 |
| `GET` | `/api/scene/production_tasks/{id}/progress` | 查询任务进度 |

核心实现：

- 任务数量不能超过来源工单未派数量。
- 未审核任务不能开工。
- 开工前检查工艺路线、关键物料批次和必要参数。
- 任务状态流转使用状态机约束和 CAS 更新。
- 任务进度由报工、完工、不良、返修等数据汇总更新。

### 6.3 生产派工单接口

Controller 建议：

```text
SceneDispatchOrderController
```

基础路径：

```text
/api/scene/dispatch_orders
```

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/scene/dispatch_orders/generate` | 根据任务和工艺路线生成工序派工 |
| `PUT` | `/api/scene/dispatch_orders/{id}/confirm` | 班组长确认派工 |
| `PUT` | `/api/scene/dispatch_orders/{id}/cancel` | 取消未执行派工 |
| `GET` | `/api/scene/dispatch_orders/{id}` | 查询派工单详情 |
| `GET` | `/api/scene/dispatch_orders/page` | 分页查询派工单 |
| `GET` | `/api/scene/dispatch_orders/{id}/operations` | 查询派工工序明细 |

核心实现：

- 从 A 组工艺管理读取工艺路线和工序信息。
- 生成工序派工明细，包含工序、工位、人员、设备、计划数量和顺序。
- 不允许跳过必检工序和关键工序。
- 工位或设备停用时不能派发新任务。

### 6.4 工序作业接口

Controller 建议：

```text
SceneOperationJobController
```

基础路径：

```text
/api/scene/operation_jobs
```

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/scene/operation_jobs/page` | 按任务、人员、工位、设备查询工序任务 |
| `GET` | `/api/scene/operation_jobs/my` | 查询当前操作员可执行工序任务 |
| `GET` | `/api/scene/operation_jobs/{id}` | 查询工序任务详情 |
| `PUT` | `/api/scene/operation_jobs/{id}/start` | 工序开工 |
| `PUT` | `/api/scene/operation_jobs/{id}/pause` | 工序暂停 |
| `PUT` | `/api/scene/operation_jobs/{id}/finish` | 工序完工 |
| `POST` | `/api/scene/operation_jobs/{id}/scan` | 扫码进入工序作业 |

核心实现：

- 扫码批次必须与任务、工序和条码应用规则匹配。
- 关键工序必须按工艺顺序执行。
- 工序完成后更新工序状态、任务进度和产品生产状态。

### 6.5 产品生产状态接口

Controller 建议：

```text
SceneProductStatusController
```

基础路径：

```text
/api/scene/product_statuses
```

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/scene/product_statuses/by_batch/{batchCode}` | 按批次查询当前生产状态 |
| `GET` | `/api/scene/product_statuses/page` | 分页查询产品状态 |
| `GET` | `/api/scene/product_statuses/{id}/histories` | 查询状态流转履历 |
| `GET` | `/api/scene/product_statuses/{id}/operation_histories` | 查询工序履历 |

核心实现：

- 产品批次开工后生成初始状态。
- 每次报工、返修、质检、异常都应记录状态变化。
- 状态履历必须按业务发生时间排序。
- 不良品进入返修或隔离后，不能直接作为良品完工。

### 6.6 生产报工接口

Controller 建议：

```text
SceneWorkReportController
```

基础路径：

```text
/api/scene/work_reports
```

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/scene/work_reports/submit` | 提交生产报工 |
| `POST` | `/api/scene/work_reports/device_count` | 接收设备计数报工数据 |
| `PUT` | `/api/scene/work_reports/{id}/correct` | 更正报工，需权限和原记录保留 |
| `PUT` | `/api/scene/work_reports/{id}/cancel` | 撤销报工，是否开放需确认 |
| `GET` | `/api/scene/work_reports/{id}` | 查询报工详情 |
| `GET` | `/api/scene/work_reports/page` | 分页查询报工记录 |
| `GET` | `/api/scene/work_reports/{id}/defects` | 查询不良报工明细 |
| `GET` | `/api/scene/work_reports/{id}/materials` | 查询关键物料报工明细 |
| `GET` | `/api/scene/work_reports/{id}/packing` | 查询装箱报工明细 |

核心实现：

- 普通报工记录任务、工序、批次、人员、设备、投入数、良品数、不良数。
- 不良报工记录不良原因、位置、处理方式。
- 关键物料报工记录产品批次与物料批次关系，是关键物料追溯的核心来源。
- 装箱报工记录箱码、中箱码、栈板码与产品批次的绑定关系。
- 数量校验必须在同一事务中完成，例如良品数 + 不良数不能超过投入数或允许范围。
- 报工成功后同步更新任务进度、工序状态、产品生产状态、工单汇总字段和报表快照。

### 6.7 生产完工单接口

Controller 建议：

```text
SceneCompletionOrderController
```

基础路径：

```text
/api/scene/completion_orders
```

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/scene/completion_orders/create_from_task` | 根据任务生成完工单 |
| `PUT` | `/api/scene/completion_orders/{id}` | 修改未审核完工单 |
| `PUT` | `/api/scene/completion_orders/{id}/submit` | 提交完工单 |
| `PUT` | `/api/scene/completion_orders/{id}/audit` | 审核完工单 |
| `GET` | `/api/scene/completion_orders/{id}` | 查询完工单详情 |
| `GET` | `/api/scene/completion_orders/page` | 分页查询完工单 |
| `GET` | `/api/scene/completion_orders/{id}/details` | 查询完工明细 |

核心实现：

- 生成完工单前校验必经工序是否完成。
- 校验必要质检是否完成且允许放行。
- 完工数量不能超过可完工数量。
- 审核后形成稳定记录，不允许直接删除。
- 后续可向 WMS/ERP 提供完工入库依据。

### 6.8 返修工单接口

Controller 建议：

```text
SceneRepairWorkOrderController
```

基础路径：

```text
/api/scene/repair_work_orders
```

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/scene/repair_work_orders` | 手工创建返修工单 |
| `POST` | `/api/scene/repair_work_orders/create_from_defect` | 从不良记录生成返修工单 |
| `PUT` | `/api/scene/repair_work_orders/{id}/assign` | 指派返修工序和责任人 |
| `PUT` | `/api/scene/repair_work_orders/{id}/start` | 开始返修 |
| `POST` | `/api/scene/repair_work_orders/{id}/records` | 提交返修记录 |
| `POST` | `/api/scene/repair_work_orders/{id}/recheck` | 提交复检结果 |
| `PUT` | `/api/scene/repair_work_orders/{id}/close` | 关闭返修工单 |
| `GET` | `/api/scene/repair_work_orders/{id}` | 查询返修工单详情 |
| `GET` | `/api/scene/repair_work_orders/page` | 分页查询返修工单 |

核心实现：

- 返修数量不能超过来源不良数量。
- 未完成复检的返修品不能计入合格完工。
- 多次返修要记录次数、原因和结果。
- 报废处理要记录责任、数量和原因。
- 产品追溯中必须能看到返修履历。

### 6.9 平板端现场接口

平板端接口可以独立 Controller，也可以复用上述 Controller。为了便于前端隔离，建议单独提供轻量聚合接口。

Controller 建议：

```text
SceneTabletController
```

基础路径：

```text
/api/scene/tablet
```

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/scene/tablet/production_tasks` | 平板端任务列表 |
| `GET` | `/api/scene/tablet/production_tasks/{id}` | 平板端任务详情 |
| `PUT` | `/api/scene/tablet/production_tasks/{id}/start` | 平板端任务开工 |
| `PUT` | `/api/scene/tablet/production_tasks/{id}/pause` | 平板端任务暂停 |
| `PUT` | `/api/scene/tablet/production_tasks/{id}/resume` | 平板端任务恢复 |
| `PUT` | `/api/scene/tablet/production_tasks/{id}/finish` | 平板端任务结束 |
| `GET` | `/api/scene/tablet/operation_jobs` | 平板端工序作业列表 |
| `POST` | `/api/scene/tablet/operation_jobs/{id}/work_report` | 平板端工序报工 |
| `GET` | `/api/scene/tablet/operation_jobs/{id}/sops` | 当前工序 SOP 展示内容 |
| `GET` | `/api/scene/tablet/product_trace` | 平板端产品追溯，可转调 `report` 追溯服务 |

## 7. `report` 报表、追溯、小程序和看板接口规划

### 7.1 产品追溯和关键物料追溯接口

Controller 建议：

```text
TraceController
```

基础路径：

```text
/api/report/traces
```

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/report/traces/products` | 按产品批次码、工单号或任务单号查询产品追溯 |
| `GET` | `/api/report/traces/materials` | 按物料批次、产品批次或工单号查询关键物料追溯 |
| `GET` | `/api/report/traces/barcodes/{barcodeValue}` | 按条码值解析并返回追溯上下文 |
| `GET` | `/api/report/traces/products/{batchCode}/timeline` | 查询批次时间线 |
| `GET` | `/api/report/traces/products/{batchCode}/materials` | 查询批次关联关键物料 |
| `GET` | `/api/report/traces/products/{batchCode}/quality` | 查询批次质量结果，转调质量模块 |
| `GET` | `/api/report/traces/products/{batchCode}/exceptions` | 查询批次异常和安灯记录，转调安灯模块 |

核心实现：

- 产品追溯以产品批次码为主键入口。
- 关键物料追溯支持正向追溯和反向追溯：
  - 产品批次 -> 物料批次。
  - 物料批次 -> 受影响产品批次。
- B 组直接读取 `scene`、`barcode` 以及 A/C 组相关业务表。
- 质量、设备、安灯信息可由 B 组报表和追溯查询直接读取 C 组结果表，但不修改 C 组核心业务状态。
- 缺失环节要明确返回 `dataCompleteness` 或类似字段，不能隐藏数据断点。

### 7.2 产量报表接口

Controller 建议：

```text
ProductionOutputReportController
```

基础路径：

```text
/api/report/production_outputs
```

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/report/production_outputs/summary` | 查询产量汇总 |
| `GET` | `/api/report/production_outputs/trend` | 查询产量趋势 |
| `GET` | `/api/report/production_outputs/details` | 查询产量明细 |
| `GET` | `/api/report/production_outputs/export` | 同步导出产量报表，UTF-8 CSV，最多 31 天和 10000 行 |

统计维度：

- 日期范围。
- 日、周、月。
- 车间。
- 产线。
- 产品。
- 班次。
- 工单。

统计指标：

- 计划数量。
- 投入数量。
- 良品数量。
- 不良数量。
- 完工数量。
- 达成率。
- 不良率。

### 7.3 生产实时信息接口

Controller 建议：

```text
RealtimeProductionController
```

基础路径：

```text
/api/report/realtime_production
```

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/report/realtime_production/overview` | 查询实时生产总览 |
| `GET` | `/api/report/realtime_production/tasks` | 查询当前在制任务 |
| `GET` | `/api/report/realtime_production/lines` | 查询产线实时状态 |
| `GET` | `/api/report/realtime_production/tasks/{taskId}/operations` | 查询任务工序明细 |

核心实现：

- 只显示未完工、未关闭的生产任务。
- 返回最后刷新时间。
- 异常任务需要突出标识。
- 第一阶段可使用普通 HTTP 查询；如刷新频率高，再引入 Redis 快照或定时聚合。

### 7.4 不良查询接口

Controller 建议：

```text
DefectQueryController
```

基础路径：

```text
/api/report/defects
```

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/report/defects/page` | 分页查询不良明细 |
| `GET` | `/api/report/defects/summary` | 查询不良汇总 |
| `GET` | `/api/report/defects/reason_ranking` | 查询不良原因排名 |
| `GET` | `/api/report/defects/trend` | 查询不良趋势 |

核心实现：

- 来源包括 B 组报工不良、C 组质量检验不良、B 组返修结果。
- 同一不良不能重复统计。
- 不良原因为空的数据归为“未分类”。
- 明细必须能追溯到来源报工或检验单。

### 7.5 车间生产时段报表接口

Controller 建议：

```text
WorkshopPeriodReportController
```

基础路径：

```text
/api/report/workshop_periods
```

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/report/workshop_periods/summary` | 查询车间时段汇总 |
| `GET` | `/api/report/workshop_periods/comparison` | 对比不同车间或不同时段 |
| `GET` | `/api/report/workshop_periods/details` | 查询时段明细 |
| `GET` | `/api/report/workshop_periods/export` | 导出车间时段报表 |

核心指标：

- 车间产量。
- 计划达成率。
- 不良率。
- 异常次数。
- 停线时长。
- 设备状态摘要。

### 7.6 微信小程序聚合接口

Controller 建议：

```text
MiniAppDashboardController
```

基础路径：

```text
/api/report/mini_app
```

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/report/mini_app/realtime_dashboard` | 小程序实时看板 |
| `GET` | `/api/report/mini_app/production_analysis` | 小程序生产分析 |
| `GET` | `/api/report/mini_app/product_trace` | 小程序产品追溯 |
| `GET` | `/api/report/mini_app/tasks/{taskId}` | 小程序任务详情，可选 |

核心实现：

- 小程序接口以查询展示为主，不承担复杂维护功能。
- 按用户权限过滤可查看车间或产线。
- 指标口径必须与后台报表一致。
- 响应结构面向移动端简化，不直接暴露多个模块原始表结构。

### 7.7 电子看板聚合接口

Controller 建议：

```text
KanbanController
```

基础路径：

```text
/api/report/kanban
```

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/report/kanban/lines/{lineId}` | 产线看板 |
| `GET` | `/api/report/kanban/workshops/{workshopId}` | 车间看板 |
| `GET` | `/api/report/kanban/central` | 中控看板 |
| `GET` | `/api/report/kanban/refresh_config` | 查询刷新配置，可选 |

核心实现：

- 产线看板优先展示当前正在生产的任务。
- 车间看板聚合车间内所有产线。
- 中控看板聚合全厂生产、质量、设备、异常、能耗等指标。
- 数据刷新失败时返回最后更新时间和数据状态。
- 如果刷新频率高，建议用 Redis 保存短 TTL 快照。

## 8. 数据模型建议

### 8.1 `barcode` 表建议

| 表名 | 说明 |
| --- | --- |
| `barcode_type` | 条码类型，产品码、箱码、栈板码、材料码等 |
| `barcode_rule` | 条码规则主表 |
| `barcode_rule_segment` | 条码规则组成项，常量、日期、变量、流水号等 |
| `barcode_template` | 标签模板主表 |
| `barcode_template_field` | 标签模板字段配置 |
| `barcode_template_version` | 模板版本记录 |
| `barcode_application_rule` | 产品/物料与条码类型、规则、模板的绑定关系 |
| `barcode_instance` | 条码实例或条码主表，记录条码值和业务对象 |
| `barcode_print_record` | 打印记录 |
| `barcode_use_record` | 扫码使用记录 |

### 8.2 `scene` 表建议

| 表名 | 说明 |
| --- | --- |
| `scene_production_parameter` | 现场生产参数 |
| `scene_parameter_scope` | 参数适用范围，车间/产线/产品 |
| `scene_parameter_change_log` | 参数变更日志 |
| `scene_production_task` | 生产任务单主表 |
| `scene_production_task_item` | 任务产品/批次明细 |
| `scene_task_progress` | 任务进度汇总 |
| `scene_task_operation_record` | 任务开工、暂停、恢复、结束操作记录 |
| `scene_dispatch_order` | 工序派工主表 |
| `scene_dispatch_operation` | 工序派工明细 |
| `scene_operation_execution_record` | 工序执行记录 |
| `scene_product_status` | 产品批次当前生产状态 |
| `scene_status_change_record` | 状态流转记录 |
| `scene_operation_history` | 工序履历 |
| `scene_work_report` | 生产报工主表 |
| `scene_work_report_quantity_detail` | 报工数量明细 |
| `scene_defect_report_detail` | 不良报工明细 |
| `scene_packing_report_detail` | 装箱报工明细 |
| `scene_material_trace_record` | 关键物料批次绑定记录 |
| `scene_completion_order` | 生产完工单主表 |
| `scene_completion_detail` | 完工明细 |
| `scene_completion_audit_record` | 完工审核记录 |
| `scene_repair_work_order` | 返修工单 |
| `scene_repair_record` | 返修作业记录 |
| `scene_repair_recheck_record` | 返修复检记录 |

### 8.3 `report` 表和缓存建议

报表模块第一阶段不一定需要大量实体表，可以直接基于 `scene`、`barcode`、`production` 和 C 组质量/设备/安灯结果表聚合。性能需求出现后，再增加以下结构：

| 表/缓存 | 说明 | 是否必需 |
| --- | --- | --- |
| `report_production_output_stat` | 产量统计宽表 | 可选 |
| `report_workshop_period_stat` | 车间时段统计宽表 | 可选 |
| `report_trace_query_log` | 追溯查询日志 | 可选 |
| Redis `report:realtime:*` | 实时看板短 TTL 快照 | 可选，刷新频率高时建议使用 |
| Redis `report:kanban:*` | 电子看板短 TTL 快照 | 可选，刷新频率高时建议使用 |

## 9. 核心实现策略

### 9.1 现场执行写入事务

生产报工、完工、返修这类接口属于强一致写入，应在一个事务中完成核心业务状态变化。

以生产报工为例，建议顺序如下：

```text
1. 校验任务存在且状态允许报工
2. 校验工序任务存在且状态允许报工
3. 校验批次码/条码与任务、工序匹配
4. 校验数量合法性
5. 保存 scene_work_report 主表
6. 保存数量、不良、装箱、关键物料等明细
7. 更新工序进度和状态
8. 更新任务进度
9. 更新产品生产状态和状态履历
10. 必要时更新 prod_work_order 汇总字段
11. 删除或刷新相关 Redis 缓存/快照
```

### 9.2 条码生成并发控制

条码生成建议采用“Redis 流水 + MySQL 唯一索引兜底”的方式：

```text
1. 校验条码应用规则启用
2. 校验条码类型、规则、模板启用
3. 根据规则维度构造 Redis 流水 Key
4. 使用 Redis INCR 获取流水号
5. 拼接条码值并预校验格式
6. 插入 barcode_instance
7. 如果唯一索引冲突，按有限次数重试或返回业务错误
```

Redis Key 示例：

```text
barcode:sequence:{ruleCode}:{yyyyMMdd}:{productCode}
```

### 9.3 产品追溯实现

产品追溯建议由 `TraceQueryService` 编排，返回稳定的追溯响应结构。

查询链路：

```text
批次码/条码
  -> barcode_instance / barcode_use_record
  -> scene_production_task
  -> scene_operation_history
  -> scene_work_report
  -> scene_material_trace_record
  -> scene_repair_work_order / scene_repair_record
  -> quality 查询接口
  -> equipment 查询接口
  -> andon 查询接口
```

追溯响应中建议包含：

- 批次基本信息。
- 生产任务信息。
- 工序时间线。
- 报工记录。
- 关键物料批次。
- 质量检验结果。
- 返修记录。
- 安灯/异常记录。
- 数据完整性提示。

### 9.4 报表实现

报表建议分两阶段实现：

第一阶段：直接查询业务表。

- 优点：开发快，适合课程项目闭环。
- 适合产量报表、不良查询、车间时段报表的初版。

第二阶段：增加统计宽表或 Redis 快照。

- 触发条件：查询慢、看板刷新频繁、导出数据量大。
- 可通过定时任务或写入后同步更新统计表。

### 9.5 小程序和看板聚合

小程序和电子看板都放在 `report` 模块，但响应结构应分开设计：

- 小程序响应偏轻量，适合移动端展示和下钻。
- 电子看板响应偏大屏展示，强调实时指标、排行、异常高亮和刷新时间。

两者可以复用底层 `DashboardQueryService`、`KanbanQueryService`、`TraceQueryService`，但不建议让前端直接复用同一个大而全响应对象。

## 10. 推荐开发顺序

### 10.1 第一阶段：基础表和骨架

1. 新建 `barcode`、`scene`、`report` 三个模块包。
2. 新增模块错误码常量类。
3. 新增 Flyway 迁移脚本，先建最小闭环表。
4. 建立 Entity、Repository、VO、Convert、Service、Controller 基础骨架。

### 10.2 第二阶段：条码基础能力

1. 条码类型 CRUD。
2. 条码规则 CRUD 和预览。
3. 条码应用规则 CRUD。
4. 条码批量生成。
5. 条码解析。

### 10.3 第三阶段：现场执行闭环

1. 生产任务单创建、审核、下发、开工。
2. 根据任务生成工序派工。
3. 工序扫码和报工。
4. 产品生产状态和履历更新。
5. 生产完工单生成和审核。
6. 返修工单创建、返修、复检、关闭。

### 10.4 第四阶段：追溯和报表

1. 产品追溯。
2. 关键物料追溯。
3. 产量报表。
4. 生产实时信息。
5. 不良查询。
6. 车间生产时段报表。

### 10.5 第五阶段：展示聚合接口

1. 微信小程序实时看板。
2. 微信小程序生产分析。
3. 微信小程序产品追溯。
4. 产线看板。
5. 车间看板。
6. 中控看板。

## 11. 测试与验证建议

### 11.1 单元测试重点

- 条码规则预览和生成。
- 条码流水并发生成。
- 生产任务状态流转。
- 报工数量校验。
- 报工后任务进度、工序状态、产品状态更新。
- 完工条件校验。
- 返修数量和复检逻辑。
- 追溯链路组装。
- 报表统计口径。

### 11.2 Controller 测试重点

- 参数校验失败能返回统一错误结构。
- 分页接口空结果返回空集合。
- 详情不存在返回业务错误码。
- 状态不允许时返回明确业务错误。

### 11.3 Redis 测试重点

- 条码流水 Key 是否按规则、日期、产品隔离。
- 看板快照 TTL 是否生效。
- 缓存删除后是否能从数据库回源。

## 12. 待确认问题

以下问题不阻塞第一版文档，但会影响最终接口字段、状态机和表结构：

1. `backend-java` 是否需要从 Spring Boot `4.0.7` 升级到项目约束中的 `4.1.x`？
2. A 组和 B 组在“生产任务单、生产派工单”上的边界是否按“计划/生成归 A，执行/状态/报工归 B”拆分？
3. 当前是否会建设统一登录和权限模块？如果暂时没有，B 组是否继续沿用 `DEFAULT_OPERATOR_ID` 之类的临时操作人？
4. 条码追溯粒度是一批一码为主，还是需要支持一物一码？
5. 条码打印是否需要真实对接打印服务，还是第一阶段只记录打印动作和返回预览数据？
6. 报工是否允许撤销？如果允许，是直接冲销、生成反向记录，还是走审核更正？
7. 完工单是否必须经过审核？审核通过后是否需要同步 ERP/WMS？
8. 不良数据最终以质量模块为主，还是报工不良和质检不良各自保留再由报表聚合？
9. 小程序和电子看板刷新频率是多少？是否需要 WebSocket/SSE，还是普通 HTTP 轮询即可？
10. 报表导出已确定为必做功能，并统一采用同步导出；第一阶段不设计异步导出任务。

## 13. 文档依据

本文档主要依据以下内容整理：

- `backend-java/build.gradle`
- `backend-java/settings.gradle`
- `backend-java/src/main/resources/application.yml`
- `backend-java/src/main/resources/db/migration/V1__init_production_sample.sql`
- `backend-java/src/main/java/com/badminton/mes/common/core/CommonResult.java`
- `backend-java/src/main/java/com/badminton/mes/module/production/controller/WorkOrderController.java`
- `backend-java/src/main/java/com/badminton/mes/module/production/service/impl/WorkOrderServiceImpl.java`
- `badminton-mes.wiki/14-后端模块分工规划.md`
- `badminton-mes.wiki/02-条码应用需求分析.md`
- `badminton-mes.wiki/03-现场管理需求分析.md`
- `badminton-mes.wiki/10-微信小程序需求分析.md`
- `badminton-mes.wiki/11-报表分析需求分析.md`
- `badminton-mes.wiki/12-电子看板需求分析.md`
