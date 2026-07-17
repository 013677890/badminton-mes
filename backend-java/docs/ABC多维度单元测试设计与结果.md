<!-- @author 范家权 -->

# A/B/C 多维度单元测试设计与结果

> @author 范家权  
> 分支：`main`  
> 测试日期：2026-07-17

## 1. 范围和约束

A/B/C 的模块边界以 `badminton-mes.wiki/14-后端模块分工规划.md` 为准：

| 组别 | 当前工程模块 | 说明 |
| --- | --- | --- |
| A | `production`、`craft`、`wage`、`integration` | 计划工艺、计件工资和 ERP/API |
| B | `barcode`、`scene`、`report` | 条码、现场执行、追溯报表和看板聚合 |
| C | `equipment`、`device`、`quality`、`andon` | 设备、设备接入、质量和安灯 |

本轮在 `main` 分支新增测试源文件和测试文档，不修改生产代码、数据库迁移或构建配置。

## 2. 独立维度目录

每个组别的四个维度均使用独立文件，避免把正常、边界、异常和权限/并发混在同一个测试类中：

```text
backend-java/src/test/java/com/badminton/mes/group/
├─ a/
│  ├─ ATeamNormalPathTest.java
│  ├─ ATeamBoundaryTest.java
│  ├─ ATeamExceptionTest.java
│  ├─ ATeamPermissionConcurrencyTest.java
│  ├─ ATeamUnitPermissionBoundaryTest.java
│  └─ ATeamCacheKeyContractTest.java
├─ b/
│  ├─ BTeamNormalPathTest.java
│  ├─ BTeamBoundaryTest.java
│  ├─ BTeamExceptionTest.java
│  ├─ BTeamPermissionConcurrencyTest.java
│  └─ BTeamCacheKeyContractTest.java
└─ c/
   ├─ CTeamNormalPathTest.java
   ├─ CTeamBoundaryTest.java
   ├─ CTeamExceptionTest.java
   ├─ CTeamPermissionConcurrencyTest.java
   ├─ CTeamConversionNormalPathTest.java
   ├─ CTeamConversionBoundaryTest.java
   └─ CTeamCacheKeyContractTest.java
```

### 2.1 正常路径

覆盖各组核心功能的可用输入和稳定输出：

- A：工单活动状态、工艺版本一致、计件金额、ERP 状态编码；
- B：条码常量/日期/变量/流水组合、生产参数默认值、任务/工序/批次状态和看板快照；
- C：设备类别、故障原理、设备台账、保养计划/记录、报修、设备联调/计数异常、质量分类/项目/检验单、安灯类型/配置/原因的转换契约。

### 2.2 边界条件

覆盖空集合、可选字段、零值、最大流水、64 字节条码上限、终态集合和可空快照字段。

### 2.3 异常处理

覆盖版本冲突、金额精度错误、ERP 生产模式保护、条码变量/日期/类型/容量错误、安灯规则
不完整和质量方案编码重复。断言统一业务错误码或稳定异常类型，并验证失败前不写库。

### 2.4 权限/并发

覆盖未登录拒绝、管理员范围放行、车间/产线数据范围、ThreadLocal 操作人隔离，以及金额、
条码组合器和 C 组转换器在并发调用下无共享可变状态污染。

### 2.5 B 组报表、缺陷来源与看板补充测试

本轮为 B 组 `report` 模块增加 8 个独立单元测试文件。各文件只验证一个业务组件或数据契约，
避免把服务聚合、缓存降级、值对象与缓存键测试混为一个大测试类：

| 测试文件 | 用例数 | 设计与作用 |
| --- | ---: | --- |
| `KanbanRedisKeyConstantsTest` | 3 | 验证全局空范围、车间/产线命名空间隔离和 90 秒过期策略 |
| `ReportExportFileTest` | 3 | 验证文件元数据，以及构造和读取两个方向的字节数组防御性复制 |
| `DefectSourceDtoContractTest` | 3 | 验证空集合归一化、不可变复制及缺陷净数量的正/零/负边界 |
| `ReportQueryCriteriaContractTest` | 3 | 验证 11 个查询维度传递、可选条件和值对象相等语义 |
| `RealtimeProductionServiceImplTest` | 3 | 验证实时总览聚合、任务行映射和空数量兼容 |
| `RepairDefectSourceProviderTest` | 3 | 验证继续返修、报废、复检通过以及缺失任务和条数上限 |
| `SceneDefectSourceProviderTest` | 1 | 验证查询条件及上限原样委托，且不虚构降级警告 |
| `KanbanSnapshotServiceImplTest` | 3 | 验证缓存命中、损坏 JSON 回源、Redis 写失败降级和范围登记 |

另新增 `ReportConcurrencyStressTest`，把缺陷净数量、缺陷批次防御性复制、看板缓存键和报表
查询条件拆成 4 个独立压力场景，每个场景默认执行 10,000 次操作。

## 3. 与既有功能测试的关系

既有模块测试继续承担具体 Service/Controller 功能的细粒度验证，例如：

- A 组已有生产、工艺、计件工资、ERP/API 的 Service、Controller、迁移和 Repository 测试；
- B 组已有条码 Controller/Service/Redis 测试，以及现场任务、派工和作业测试；
- C 组已有设备类别/制造商、设备计数、质量方案转换和安灯类型转换测试；本轮进一步补充其余设备、设备接入、质量和安灯转换器，以及关键重复编码服务门禁。

按维度组织的补充套件不替换既有功能测试。`report` 当前已落地追溯、报表、小程序和看板相关
服务，本轮已将实时生产、缺陷来源、看板快照、导出文件、查询条件以及并发隔离纳入 B 组
功能、边界、降级、不可变性和压力测试。

## 4. 执行结果

### 4.1 聚焦测试

```powershell
.\gradlew.bat test --tests "com.badminton.mes.group.*" --no-daemon --max-workers=2
```

结果：`BUILD SUCCESSFUL`；维度测试加压力执行器测试共 74 个用例，0 失败、0 错误、0 跳过。

报表补充测试执行命令：

```powershell
.\gradlew.bat test --tests "com.badminton.mes.module.report.*" --no-daemon --max-workers=2
```

结果：`BUILD SUCCESSFUL`；16 个报表测试类、39 个用例，0 失败、0 错误、0 跳过。本轮新增
8 个测试类、22 个用例。

### 4.2 压力测试

压力套件按组别和模块使用独立文件：

```text
backend-java/src/test/java/com/badminton/mes/stress/
├─ ATeamConcurrencyStressTest.java
├─ BTeamConcurrencyStressTest.java
├─ CTeamConcurrencyStressTest.java
├─ ModuleConcurrencyStressTest.java
└─ ReportConcurrencyStressTest.java
```

执行命令：

```powershell
.\gradlew.bat stressTest --no-daemon --max-workers=2
```

结果：`BUILD SUCCESSFUL`；5 个压力测试类、25 个压力场景，0 失败、0 错误、0 跳过。新增的
4 个报表场景默认各执行 10,000 次操作；完整套件覆盖无状态业务规则、条码组合、登录上下文
隔离、缺陷批次不可变性、看板缓存键、查询条件以及各模块状态和转换契约。

### 4.3 全量门禁

```powershell
.\gradlew.bat test --no-daemon --max-workers=2
```

执行结果：`BUILD SUCCESSFUL`；144 个测试类、805 个用例，0 失败、0 错误、0 跳过。

全量门禁期间还修正了一个已有安灯权限测试夹具：原用例声称“无指派角色”，但测试事件和
测试用户都使用 `OPERATOR`，导致服务按指派角色正确放行。现将事件指派角色改为
`TEAM_LEADER`，使测试数据与拒绝场景一致；生产代码未修改。

该命令应在提交前执行，确认新增维度套件没有影响既有模块测试。若某次结果包含集成环境
相关失败，应分开记录外部 MySQL/Redis 阻塞，不得把单元测试结果替代集成验证。

## 5. 后续扩展规则

新增 A/B/C 功能时，按同一功能至少补齐四类独立文件；若功能有真实数据库或 Redis 依赖，
另加 `@Tag("integration")` 文件；若需进程内并发基线，另加 `@Tag("stress")` 文件。所有
新文件的类级 Javadoc 均使用 `@author 范家权`。
