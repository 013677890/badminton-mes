<!-- @author 范家权 -->

# A/B/C 多维度单元测试设计与结果

> @author 范家权
> 分支：`main`
> 测试日期：2026-07-16

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

## 3. 与既有功能测试的关系

既有模块测试继续承担具体 Service/Controller 功能的细粒度验证，例如：

- A 组已有生产、工艺、计件工资、ERP/API 的 Service、Controller、迁移和 Repository 测试；
- B 组已有条码 Controller/Service/Redis 测试，以及现场任务、派工和作业测试；
- C 组已有设备类别/制造商、设备计数、质量方案转换和安灯类型转换测试；本轮进一步补充其余设备、设备接入、质量和安灯转换器，以及关键重复编码服务门禁。

按维度组织的补充套件不替换既有功能测试。`report` 当前已落地追溯、报表、小程序和看板相关
服务，本轮将看板快照 Key 和并发隔离纳入 B 组压力与缓存契约测试。

## 4. 执行结果

### 4.1 聚焦测试

```powershell
.\gradlew.bat test --tests "com.badminton.mes.group.*" --no-daemon --max-workers=2
```

结果：`BUILD SUCCESSFUL`；维度测试加压力执行器测试共 74 个用例，0 失败、0 错误、0 跳过。

### 4.2 压力测试

新增三个独立压力测试文件：

```text
backend-java/src/test/java/com/badminton/mes/stress/
├─ ATeamConcurrencyStressTest.java
├─ BTeamConcurrencyStressTest.java
├─ CTeamConcurrencyStressTest.java
└─ ConcurrentStressRunnerTest.java
```

执行命令：

```powershell
.\gradlew.bat stressTest --no-daemon --max-workers=2
```

结果：`BUILD SUCCESSFUL`；4 个压力测试类、21 个压力场景，0 失败。新增的 10 个 A/B/C 场景
默认各执行 10,000 次操作，覆盖无状态业务规则、条码组合、登录上下文隔离、看板快照、缓存
命名空间与版本 Key。

### 4.3 全量门禁

```powershell
.\gradlew.bat test --no-daemon --max-workers=2
```

执行结果：`BUILD SUCCESSFUL`；136 个测试类、783 个用例，0 失败、0 错误、0 跳过。

全量门禁期间还修正了一个已有安灯权限测试夹具：原用例声称“无指派角色”，但测试事件和
测试用户都使用 `OPERATOR`，导致服务按指派角色正确放行。现将事件指派角色改为
`TEAM_LEADER`，使测试数据与拒绝场景一致；生产代码未修改。

该命令应在提交前执行，确认新增维度套件没有影响既有模块测试。若某次结果包含集成环境
相关失败，应分开记录外部 MySQL/Redis 阻塞，不得把单元测试结果替代集成验证。

## 5. 后续扩展规则

新增 A/B/C 功能时，按同一功能至少补齐四类独立文件；若功能有真实数据库或 Redis 依赖，
另加 `@Tag("integration")` 文件；若需进程内并发基线，另加 `@Tag("stress")` 文件。所有
新文件的类级 Javadoc 均使用 `@author 范家权`。
