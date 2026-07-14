# B组现场执行与追溯报表接口文档

> 编写日期：2026-07-09  
> 适用范围：B 组 `barcode`、`scene`、`report` 后端接口  
> 当前决策：Spring Boot 保持当前 `4.0.7`；统一登录与权限模块存在；条码按“一批一码”；打印只记录动作并返回预览；报工撤销采用“生成反向记录”；完工单必须审核且审核后同步；看板一分钟刷新一次并使用 WebSocket；B 组允许直接查询 A/C 组相关业务表；不引入 Kafka/RabbitMQ；报表导出统一采用同步导出；Redis Pub/Sub 仅作为多实例 WebSocket 或缓存通知的可选补充，不作为可靠任务队列。

## 1. 接口总体约定

### 1.1 模块路径

B 组后端接口建议分为三类基础路径：

| 模块 | 基础路径 | 说明 |
| --- | --- | --- |
| 条码应用 | `/api/barcode` | 条码类型、规则、模板、应用规则、条码生成、打印记录、条码解析 |
| 现场执行 | `/api/scene` | 生产任务、工序派工、工序作业、报工、完工、返修、平板端接口 |
| 报表追溯 | `/api/report` | 追溯、产量报表、实时生产、不良查询、车间时段、小程序、电子看板、导出 |

### 1.2 通用返回结构

沿用当前 `backend-java` 已有统一响应：

```json
{
  "code": "00000",
  "message": "success",
  "userTip": null,
  "data": {}
}
```

所有接口返回：

```java
CommonResult<T>
```

分页接口返回：

```java
CommonResult<PageResult<XxxRespVO>>
```

### 1.3 通用认证与权限

项目存在统一登录和权限模块，B 组接口应遵守以下规则：

- 写接口从登录上下文获取操作人，不再使用临时 `DEFAULT_OPERATOR_ID`。
- 查询接口按用户的数据权限过滤车间、产线、班组和任务范围。
- 平板端接口按人员、工位、产线权限过滤可操作任务。
- 小程序和看板接口按用户或看板绑定范围过滤数据。
- 审核、撤销、冲销、导出等敏感动作需要独立权限点。

### 1.4 通用命名约定

| 类型 | 命名建议 |
| --- | --- |
| Controller | `XxxController` |
| Service | `XxxService`、`XxxServiceImpl` |
| 请求 VO | `XxxSaveReqVO`、`XxxPageReqVO`、`XxxSubmitReqVO`、`XxxAuditReqVO` |
| 响应 VO | `XxxRespVO` |
| 内部 DTO | `XxxDTO` |
| Entity | `XxxEntity` |
| Repository | `XxxRepository` |
| Convert | `XxxConvert` |
| 错误码 | `BarcodeErrorCodeConstants`、`SceneErrorCodeConstants`、`ReportErrorCodeConstants` |

### 1.5 通用状态建议

本文档中的状态值以接口语义为主，最终编码可在枚举中统一定义。

| 对象 | 状态建议 |
| --- | --- |
| 条码实例 | 未使用、已使用、已打印、已作废 |
| 生产任务 | 草稿、待审核、已审核、已下发、生产中、暂停、已完成、已关闭、已取消 |
| 工序任务 | 待作业、作业中、暂停、已完成、异常、已取消 |
| 报工记录 | 正常、已冲销、冲销记录 |
| 完工单 | 草稿、待审核、审核通过、审核驳回、已同步、同步失败 |
| 返修工单 | 待指派、待返修、返修中、待复检、已放行、继续返修、已报废、已关闭 |

## 2. 条码应用接口

### 2.1 条码类型接口

Controller：`BarcodeTypeController`  
基础路径：`/api/barcode/types`

| 方法 | 路径 | 接口说明 | 主要请求对象 | 主要响应对象 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/barcode/types` | 新增条码类型 | `BarcodeTypeSaveReqVO` | `Long` |
| `PUT` | `/api/barcode/types/{id}` | 修改条码类型 | `BarcodeTypeSaveReqVO` | `Void` |
| `PUT` | `/api/barcode/types/{id}/enable` | 启用条码类型 | - | `Void` |
| `PUT` | `/api/barcode/types/{id}/disable` | 停用条码类型 | - | `Void` |
| `DELETE` | `/api/barcode/types/{id}` | 删除条码类型 | - | `Void` |
| `GET` | `/api/barcode/types/{id}` | 查询条码类型详情 | - | `BarcodeTypeRespVO` |
| `GET` | `/api/barcode/types/page` | 分页查询条码类型 | `BarcodeTypePageReqVO` | `PageResult<BarcodeTypeRespVO>` |
| `GET` | `/api/barcode/types/options` | 查询启用条码类型选项 | - | `List<BarcodeTypeRespVO>` |

核心规则：

- 条码类型编码唯一。
- 已被条码规则或应用规则使用的类型不允许删除。
- 停用后不允许新建相关应用规则。

### 2.2 条码规则接口

Controller：`BarcodeRuleController`  
基础路径：`/api/barcode/rules`

| 方法 | 路径 | 接口说明 | 主要请求对象 | 主要响应对象 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/barcode/rules` | 新增条码规则 | `BarcodeRuleSaveReqVO` | `Long` |
| `PUT` | `/api/barcode/rules/{id}` | 修改条码规则 | `BarcodeRuleSaveReqVO` | `Void` |
| `PUT` | `/api/barcode/rules/{id}/enable` | 启用规则 | - | `Void` |
| `PUT` | `/api/barcode/rules/{id}/disable` | 停用规则 | - | `Void` |
| `DELETE` | `/api/barcode/rules/{id}` | 删除未使用规则 | - | `Void` |
| `GET` | `/api/barcode/rules/{id}` | 查询规则详情 | - | `BarcodeRuleRespVO` |
| `GET` | `/api/barcode/rules/page` | 分页查询规则 | `BarcodeRulePageReqVO` | `PageResult<BarcodeRuleRespVO>` |
| `POST` | `/api/barcode/rules/preview` | 预览规则生成结果 | `BarcodeRulePreviewReqVO` | `BarcodeRulePreviewRespVO` |
| `POST` | `/api/barcode/rules/validate` | 校验规则合法性 | `BarcodeRuleValidateReqVO` | `BarcodeRuleValidateRespVO` |

核心规则：

- 一批一码场景下，优先支持“产品编码 + 日期 + 批次流水号”。
- 规则修改只影响新生成条码，不影响历史条码。
- 流水号建议使用 Redis `INCR`，最终唯一性由 MySQL 唯一索引兜底。

### 2.3 条码模板接口

Controller：`BarcodeTemplateController`  
基础路径：`/api/barcode/templates`

| 方法 | 路径 | 接口说明 | 主要请求对象 | 主要响应对象 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/barcode/templates` | 新增标签模板 | `BarcodeTemplateSaveReqVO` | `Long` |
| `PUT` | `/api/barcode/templates/{id}` | 修改标签模板 | `BarcodeTemplateSaveReqVO` | `Void` |
| `PUT` | `/api/barcode/templates/{id}/enable` | 启用模板 | - | `Void` |
| `PUT` | `/api/barcode/templates/{id}/disable` | 停用模板 | - | `Void` |
| `GET` | `/api/barcode/templates/{id}` | 查询模板详情 | - | `BarcodeTemplateRespVO` |
| `GET` | `/api/barcode/templates/page` | 分页查询模板 | `BarcodeTemplatePageReqVO` | `PageResult<BarcodeTemplateRespVO>` |
| `POST` | `/api/barcode/templates/preview` | 返回模板预览数据 | `BarcodeTemplatePreviewReqVO` | `BarcodeTemplatePreviewRespVO` |

当前决策：第一阶段不对接真实打印机，只保存模板配置并返回打印预览数据。

### 2.4 条码应用规则接口

Controller：`BarcodeApplicationRuleController`  
基础路径：`/api/barcode/application_rules`

| 方法 | 路径 | 接口说明 | 主要请求对象 | 主要响应对象 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/barcode/application_rules` | 新增应用规则 | `BarcodeApplicationRuleSaveReqVO` | `Long` |
| `PUT` | `/api/barcode/application_rules/{id}` | 修改应用规则 | `BarcodeApplicationRuleSaveReqVO` | `Void` |
| `PUT` | `/api/barcode/application_rules/{id}/enable` | 启用应用规则 | - | `Void` |
| `PUT` | `/api/barcode/application_rules/{id}/disable` | 停用应用规则 | - | `Void` |
| `DELETE` | `/api/barcode/application_rules/{id}` | 删除未使用应用规则 | - | `Void` |
| `GET` | `/api/barcode/application_rules/{id}` | 查询应用规则详情 | - | `BarcodeApplicationRuleRespVO` |
| `GET` | `/api/barcode/application_rules/page` | 分页查询应用规则 | `BarcodeApplicationRulePageReqVO` | `PageResult<BarcodeApplicationRuleRespVO>` |
| `GET` | `/api/barcode/application_rules/options` | 查询可用应用规则选项 | `BarcodeApplicationRuleOptionReqVO` | `List<BarcodeApplicationRuleRespVO>` |

核心规则：

- 同一产品或物料、同一条码类型只允许一条启用默认规则。
- 启用前校验条码类型、条码规则和标签模板均处于启用状态。
- 当前实现优先支持批次码，即“一批一码”。

### 2.5 条码生成、打印、解析接口

Controller：`BarcodeInstanceController`  
基础路径：`/api/barcode/instances`

| 方法 | 路径 | 接口说明 | 主要请求对象 | 主要响应对象 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/barcode/instances/generate` | 生成批次码 | `BarcodeGenerateReqVO` | `BarcodeGenerateRespVO` |
| `POST` | `/api/barcode/instances/batch_generate` | 批量生成批次码 | `BarcodeBatchGenerateReqVO` | `List<BarcodeGenerateRespVO>` |
| `POST` | `/api/barcode/instances/import` | 导入外部批次码 | `BarcodeImportReqVO` | `BarcodeImportRespVO` |
| `POST` | `/api/barcode/instances/{id}/print` | 记录打印动作并返回预览数据 | `BarcodePrintReqVO` | `BarcodePrintRespVO` |
| `PUT` | `/api/barcode/instances/{id}/cancel` | 作废未使用条码 | `BarcodeCancelReqVO` | `Void` |
| `GET` | `/api/barcode/instances/{id}` | 查询条码详情 | - | `BarcodeInstanceRespVO` |
| `GET` | `/api/barcode/instances/page` | 分页查询条码实例 | `BarcodeInstancePageReqVO` | `PageResult<BarcodeInstanceRespVO>` |
| `POST` | `/api/barcode/instances/parse` | 解析条码值 | `BarcodeParseReqVO` | `BarcodeParseRespVO` |
| `GET` | `/api/barcode/instances/{id}/use_records` | 查询扫码使用记录 | - | `List<BarcodeUseRecordRespVO>` |

核心规则：

- 条码值全局唯一。
- 已使用条码不能作废。
- 打印接口只记录打印次数、打印人、打印时间、打印原因和预览内容，不直接驱动打印设备。
- 扫码使用记录需要关联任务、工序、人员、设备和业务发生时间。

## 3. 现场执行接口

### 3.1 生产参数接口

Controller：`SceneProductionParameterController`  
基础路径：`/api/scene/production_parameters`

| 方法 | 路径 | 接口说明 |
| --- | --- | --- |
| `POST` | `/api/scene/production_parameters` | 新增生产参数 |
| `PUT` | `/api/scene/production_parameters/{id}` | 修改生产参数 |
| `PUT` | `/api/scene/production_parameters/{id}/enable` | 启用参数 |
| `PUT` | `/api/scene/production_parameters/{id}/disable` | 停用参数 |
| `GET` | `/api/scene/production_parameters/{id}` | 查询参数详情 |
| `GET` | `/api/scene/production_parameters/page` | 分页查询参数 |
| `GET` | `/api/scene/production_parameters/effective` | 查询当前生效参数 |
| `GET` | `/api/scene/production_parameters/{id}/change_logs` | 查询参数变更日志 |

参数范围包括是否允许超产、是否必须扫码报工、是否允许跳工序、是否启用首件检验、是否启用安灯联动等。

### 3.2 生产任务单接口

Controller：`SceneProductionTaskController`  
基础路径：`/api/scene/production_tasks`

| 方法 | 路径 | 接口说明 | 主要请求对象 | 主要响应对象 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/scene/production_tasks` | 根据已下达工单创建生产任务 | `SceneProductionTaskSaveReqVO` | `Long` |
| `PUT` | `/api/scene/production_tasks/{id}` | 修改未审核任务 | `SceneProductionTaskSaveReqVO` | `Void` |
| `PUT` | `/api/scene/production_tasks/{id}/audit` | 审核任务 | `SceneProductionTaskAuditReqVO` | `Void` |
| `PUT` | `/api/scene/production_tasks/{id}/release` | 下发任务到现场 | - | `Void` |
| `PUT` | `/api/scene/production_tasks/{id}/start` | 任务开工 | `SceneTaskStartReqVO` | `Void` |
| `PUT` | `/api/scene/production_tasks/{id}/pause` | 任务暂停 | `SceneTaskPauseReqVO` | `Void` |
| `PUT` | `/api/scene/production_tasks/{id}/resume` | 任务恢复 | `SceneTaskResumeReqVO` | `Void` |
| `PUT` | `/api/scene/production_tasks/{id}/close` | 任务关闭 | `SceneTaskCloseReqVO` | `Void` |
| `GET` | `/api/scene/production_tasks/{id}` | 查询任务详情 | - | `SceneProductionTaskRespVO` |
| `GET` | `/api/scene/production_tasks/page` | 分页查询任务 | `SceneProductionTaskPageReqVO` | `PageResult<SceneProductionTaskRespVO>` |
| `GET` | `/api/scene/production_tasks/{id}/progress` | 查询任务进度 | - | `SceneTaskProgressRespVO` |

核心规则：

- B 组只允许基于 A 组已下达工单创建现场生产任务。
- 任务数量不能超过来源工单未派数量。
- 未审核任务不能开工。
- 开工前检查工艺路线、关键物料批次和必要参数。

### 3.3 工序派工接口

Controller：`SceneDispatchOrderController`  
基础路径：`/api/scene/dispatch_orders`

| 方法 | 路径 | 接口说明 |
| --- | --- | --- |
| `POST` | `/api/scene/dispatch_orders/generate` | 根据任务和工艺路线生成工序派工 |
| `PUT` | `/api/scene/dispatch_orders/{id}/confirm` | 确认派工 |
| `PUT` | `/api/scene/dispatch_orders/{id}/cancel` | 取消未执行派工 |
| `GET` | `/api/scene/dispatch_orders/{id}` | 查询派工单详情 |
| `GET` | `/api/scene/dispatch_orders/page` | 分页查询派工单 |
| `GET` | `/api/scene/dispatch_orders/{id}/operations` | 查询派工工序明细 |

核心规则：

- 工艺路线和工序基础数据来自 A 组工艺模块查询接口。
- 工序派工属于 B 组现场执行数据。
- 已开始执行的派工不允许直接取消。

### 3.4 工序作业接口

Controller：`SceneOperationJobController`  
基础路径：`/api/scene/operation_jobs`

| 方法 | 路径 | 接口说明 |
| --- | --- | --- |
| `GET` | `/api/scene/operation_jobs/page` | 分页查询工序任务 |
| `GET` | `/api/scene/operation_jobs/my` | 查询当前操作员可执行工序 |
| `GET` | `/api/scene/operation_jobs/{id}` | 查询工序任务详情 |
| `POST` | `/api/scene/operation_jobs/{id}/scan` | 扫码进入工序作业 |
| `PUT` | `/api/scene/operation_jobs/{id}/start` | 工序开工 |
| `PUT` | `/api/scene/operation_jobs/{id}/pause` | 工序暂停 |
| `PUT` | `/api/scene/operation_jobs/{id}/finish` | 工序完工 |

核心规则：

- 扫码批次必须与任务、工序和条码应用规则匹配。
- 关键工序必须按工艺顺序执行。
- 工序完成后更新任务进度和产品生产状态。

### 3.5 产品生产状态接口

Controller：`SceneProductStatusController`  
基础路径：`/api/scene/product_statuses`

| 方法 | 路径 | 接口说明 |
| --- | --- | --- |
| `GET` | `/api/scene/product_statuses/by_batch/{batchCode}` | 按批次查询当前生产状态 |
| `GET` | `/api/scene/product_statuses/page` | 分页查询生产状态 |
| `GET` | `/api/scene/product_statuses/{id}/histories` | 查询状态流转履历 |
| `GET` | `/api/scene/product_statuses/{id}/operation_histories` | 查询工序履历 |

### 3.6 生产报工接口

Controller：`SceneWorkReportController`  
基础路径：`/api/scene/work_reports`

| 方法 | 路径 | 接口说明 | 主要请求对象 | 主要响应对象 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/scene/work_reports/submit` | 提交生产报工 | `SceneWorkReportSubmitReqVO` | `Long` |
| `POST` | `/api/scene/work_reports/device_count` | 接收设备计数报工 | `SceneDeviceCountReportReqVO` | `Long` |
| `PUT` | `/api/scene/work_reports/{id}/reverse` | 生成反向记录冲销原报工 | `SceneWorkReportReverseReqVO` | `Long` |
| `GET` | `/api/scene/work_reports/{id}` | 查询报工详情 | - | `SceneWorkReportRespVO` |
| `GET` | `/api/scene/work_reports/page` | 分页查询报工记录 | `SceneWorkReportPageReqVO` | `PageResult<SceneWorkReportRespVO>` |
| `GET` | `/api/scene/work_reports/{id}/defects` | 查询报工不良明细 | - | `List<SceneDefectReportDetailRespVO>` |
| `GET` | `/api/scene/work_reports/{id}/materials` | 查询关键物料报工明细 | - | `List<SceneMaterialTraceRecordRespVO>` |
| `GET` | `/api/scene/work_reports/{id}/packing` | 查询装箱报工明细 | - | `List<ScenePackingReportDetailRespVO>` |

`SceneWorkReportSubmitReqVO` 使用 `barcodeValue` 接收报工扫码值，不接受客户端直接指定可信
`barcodeId`。当当前任务作用域内生效参数 `must_scan_report=1` 时，条码值必填；Service 复用
M1 `BarcodeSceneService` 校验任务、产品和批次，写入 `use_type=3` 的条码使用记录，并将服务端
解析得到的 `barcodeId` 保存到报工记录。参数未要求扫码但客户端主动传入条码时仍执行相同校验和留痕。

当前决策：报工撤销采用“生成反向记录”，不物理删除原报工、不直接覆盖原数量。

反向记录要求：

- `recordType` 标记为 `REVERSAL`。
- `sourceReportId` 指向原报工记录。
- 良品数、不良数、返修数、投入数等数量字段使用负数或方向字段表达冲销。
- 报表统计时按正向记录与反向记录的净额汇总。
- 原报工记录保留完整审计链路。

### 3.7 生产完工单接口

Controller：`SceneCompletionOrderController`  
基础路径：`/api/scene/completion_orders`

| 方法 | 路径 | 接口说明 | 主要请求对象 | 主要响应对象 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/scene/completion_orders/create_from_task` | 根据任务生成完工单 | `SceneCompletionCreateReqVO` | `Long` |
| `PUT` | `/api/scene/completion_orders/{id}` | 修改草稿完工单 | `SceneCompletionSaveReqVO` | `Void` |
| `PUT` | `/api/scene/completion_orders/{id}/submit` | 提交完工单审核 | `SceneCompletionSubmitReqVO` | `Void` |
| `PUT` | `/api/scene/completion_orders/{id}/audit` | 审核完工单 | `SceneCompletionAuditReqVO` | `Void` |
| `POST` | `/api/scene/completion_orders/{id}/sync` | 手动触发外部同步 | - | `Void` |
| `GET` | `/api/scene/completion_orders/{id}` | 查询完工单详情 | - | `SceneCompletionOrderRespVO` |
| `GET` | `/api/scene/completion_orders/page` | 分页查询完工单 | `SceneCompletionOrderPageReqVO` | `PageResult<SceneCompletionOrderRespVO>` |
| `GET` | `/api/scene/completion_orders/{id}/sync_records` | 查询同步记录 | - | `List<SceneCompletionSyncRecordRespVO>` |

当前决策：完工单必须审核，审核通过后同步给外部系统或 A 组集成接口。

完工单修改接口只允许修改 `finishQuantity`。草稿和审核驳回状态可修改，待审核和审核通过状态
拒绝修改；修改时同步令 `goodQuantity=finishQuantity`，不更新任务完工汇总、不触发审核或外部同步。
任务完工数量仍只在审核通过事务中更新。

### 3.8 返修工单接口

Controller：`SceneRepairWorkOrderController`  
基础路径：`/api/scene/repair_work_orders`

| 方法 | 路径 | 接口说明 |
| --- | --- | --- |
| `POST` | `/api/scene/repair_work_orders` | 手工创建返修工单 |
| `POST` | `/api/scene/repair_work_orders/create_from_defect` | 从不良记录创建返修工单 |
| `PUT` | `/api/scene/repair_work_orders/{id}/assign` | 指派返修任务 |
| `PUT` | `/api/scene/repair_work_orders/{id}/start` | 开始返修 |
| `POST` | `/api/scene/repair_work_orders/{id}/records` | 提交返修记录 |
| `POST` | `/api/scene/repair_work_orders/{id}/recheck` | 提交返修复检结果 |
| `PUT` | `/api/scene/repair_work_orders/{id}/close` | 关闭返修工单 |
| `GET` | `/api/scene/repair_work_orders/{id}` | 查询返修工单详情 |
| `GET` | `/api/scene/repair_work_orders/page` | 分页查询返修工单 |

### 3.9 平板端接口

Controller：`SceneTabletController`  
基础路径：`/api/scene/tablet`

| 方法 | 路径 | 接口说明 |
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
| `GET` | `/api/scene/tablet/product_trace` | 平板端产品追溯 |

## 4. 报表、追溯、小程序、看板接口

### 4.1 产品追溯和关键物料追溯接口

Controller：`TraceController`  
基础路径：`/api/report/traces`

| 方法 | 路径 | 接口说明 |
| --- | --- | --- |
| `GET` | `/api/report/traces/products` | 按批次码、工单号或任务单号查询产品追溯 |
| `GET` | `/api/report/traces/materials` | 查询关键物料追溯 |
| `GET` | `/api/report/traces/barcodes/{barcodeValue}` | 按条码值解析追溯上下文 |
| `GET` | `/api/report/traces/products/{batchCode}/timeline` | 查询批次时间线 |
| `GET` | `/api/report/traces/products/{batchCode}/materials` | 查询批次关键物料 |
| `GET` | `/api/report/traces/products/{batchCode}/quality` | 查询批次质量数据 |
| `GET` | `/api/report/traces/products/{batchCode}/exceptions` | 查询批次异常数据 |

### 4.2 产量报表接口

Controller：`ProductionOutputReportController`  
基础路径：`/api/report/production_outputs`

| 方法 | 路径 | 接口说明 |
| --- | --- | --- |
| `GET` | `/api/report/production_outputs/summary` | 查询产量汇总 |
| `GET` | `/api/report/production_outputs/trend` | 查询产量趋势 |
| `GET` | `/api/report/production_outputs/details` | 查询产量明细 |
| `GET` | `/api/report/production_outputs/export` | 同步导出产量报表 |

### 4.3 生产实时信息接口

Controller：`RealtimeProductionController`  
基础路径：`/api/report/realtime_production`

| 方法 | 路径 | 接口说明 |
| --- | --- | --- |
| `GET` | `/api/report/realtime_production/overview` | 查询实时生产总览 |
| `GET` | `/api/report/realtime_production/tasks` | 查询当前在制任务 |
| `GET` | `/api/report/realtime_production/lines` | 查询产线实时状态 |
| `GET` | `/api/report/realtime_production/tasks/{taskId}/operations` | 查询任务工序明细 |

### 4.4 不良查询接口

Controller：`DefectQueryController`  
基础路径：`/api/report/defects`

| 方法 | 路径 | 接口说明 |
| --- | --- | --- |
| `GET` | `/api/report/defects/page` | 分页查询不良明细 |
| `GET` | `/api/report/defects/summary` | 查询不良汇总 |
| `GET` | `/api/report/defects/reason_ranking` | 查询不良原因排名 |
| `GET` | `/api/report/defects/trend` | 查询不良趋势 |
| `GET` | `/api/report/defects/source_comparison` | 查询报工不良与质检不良对比 |

当前决策：报工不良和质检不良各自保留，报表统一聚合。具体口径见协作文档。

### 4.5 车间时段报表接口

Controller：`WorkshopPeriodReportController`  
基础路径：`/api/report/workshop_periods`

| 方法 | 路径 | 接口说明 |
| --- | --- | --- |
| `GET` | `/api/report/workshop_periods/summary` | 查询车间时段汇总 |
| `GET` | `/api/report/workshop_periods/comparison` | 对比不同车间或不同时段 |
| `GET` | `/api/report/workshop_periods/details` | 查询时段明细 |
| `GET` | `/api/report/workshop_periods/export` | 同步导出车间时段报表 |

### 4.6 微信小程序接口

Controller：`MiniAppDashboardController`  
基础路径：`/api/report/mini_app`

| 方法 | 路径 | 接口说明 |
| --- | --- | --- |
| `GET` | `/api/report/mini_app/realtime_dashboard` | 小程序实时看板 |
| `GET` | `/api/report/mini_app/production_analysis` | 小程序生产分析 |
| `GET` | `/api/report/mini_app/product_trace` | 小程序产品追溯 |
| `GET` | `/api/report/mini_app/tasks/{taskId}` | 小程序任务详情 |

### 4.7 电子看板接口

Controller：`KanbanController`  
基础路径：`/api/report/kanban`

| 方法 | 路径 | 接口说明 |
| --- | --- | --- |
| `GET` | `/api/report/kanban/lines/{lineId}` | 产线看板 |
| `GET` | `/api/report/kanban/workshops/{workshopId}` | 车间看板 |
| `GET` | `/api/report/kanban/central` | 中控看板 |
| `GET` | `/api/report/kanban/refresh_config` | 查询刷新配置 |

### 4.8 WebSocket 推送接口

当前决策：小程序和电子看板先按一分钟刷新一次，需要 WebSocket。

建议通道：

| 通道 | 说明 |
| --- | --- |
| `/ws/report/mini_app/realtime_dashboard` | 小程序实时看板推送 |
| `/ws/report/kanban/lines/{lineId}` | 产线看板推送 |
| `/ws/report/kanban/workshops/{workshopId}` | 车间看板推送 |
| `/ws/report/kanban/central` | 中控看板推送 |

推送策略：

- 后端每分钟生成一次快照并推送。
- 连接建立后立即推送一次当前快照。
- WebSocket 失败时，前端退化为 HTTP 轮询。
- 每次推送都包含 `lastRefreshTime` 和 `dataStatus`。

### 4.9 报表导出接口

当前决策：报表导出统一采用同步导出，不再设计异步导出任务接口，也不引入 Kafka、RabbitMQ 或 Redis Pub/Sub 承载导出任务。

统一同步导出接口建议：

| 方法 | 路径 | 接口说明 |
| --- | --- | --- |
| `GET` | `/api/report/production_outputs/export` | 同步导出产量报表 |
| `GET` | `/api/report/defects/export` | 同步导出不良报表 |
| `GET` | `/api/report/workshop_periods/export` | 同步导出车间时段报表 |

同步导出实现约束：

- 项目使用 Java 21 虚拟线程时，数据库 I/O 等待不会长期占用平台线程，因此课程项目第一阶段可直接使用普通同步接口返回文件。
- 虚拟线程不提升 SQL 本身速度，也不减少数据库连接占用；导出接口仍应限制查询时间范围和最大导出行数。
- 导出查询必须复用报表查询的数据权限条件，避免越权导出其他车间、产线或班组数据。
- 导出接口保持只读，不在导出过程中修改业务状态。
- 若后续真实数据量明显超过课程项目规模，再单独评估是否补充异步导出任务表。

M4 冻结值（2026-07-13）：

- 导出格式统一为 UTF-8 CSV，并写入 BOM 以兼容常用表格软件打开中文内容；
- 单次同步导出时间范围最多 31 天；
- 单次同步导出最多 10000 行，第 10001 行用于服务端超限判断，不截断后静默返回；
- 产量和车间时段导出复用 `ReportQueryReqVO` 的时间、车间、产线、产品、工单、任务、批次、工序、班次和状态条件；
- 导出权限限 `ADMIN`、`PMC`、`WORKSHOP_MANAGER`，并继续应用登录用户车间/产线范围；
- 综合不良率分母使用同一查询范围内的报工投入数量净额，分母为 0 时返回 0。

M4 已实现的实际接口：

| 方法 | 路径 | 当前实现 |
| --- | --- | --- |
| `GET` | `/api/report/traces/products` | 支持 `batchCode`、`barcodeValue`、`workOrderNo`、`taskNo` 任一入口 |
| `GET` | `/api/report/traces/barcodes/{barcodeValue}` | 按条码反查同一产品追溯主链路 |
| `GET` | `/api/report/production_outputs/summary` | 返回净额、发生额、冲销额、达成率和不良率 |
| `GET` | `/api/report/production_outputs/details` | 分页返回正常/冲销记录及每项数量净额 |
| `GET` | `/api/report/production_outputs/export` | 同步 CSV 导出，31 天/10000 行保护 |
| `GET` | `/api/report/realtime_production/overview` | 在制任务实时汇总和可选来源警告 |
| `GET` | `/api/report/realtime_production/tasks` | 当前授权范围内的在制任务列表 |
| `GET` | `/api/report/defects/page` | `view=SOURCE` 来源明细；`view=COMPREHENSIVE` 按 `defectGroupNo` 归并 |
| `GET` | `/api/report/defects/summary` | B/C/返修来源数量、综合数量和各自比率 |
| `GET` | `/api/report/defects/export` | 同步导出不良来源明细 |
| `GET` | `/api/report/workshop_periods/summary` | 复用产量净额和数据权限口径 |
| `GET` | `/api/report/workshop_periods/details` | 复用报工审计明细 |
| `GET` | `/api/report/workshop_periods/export` | 同步导出车间时段明细 |

## 5. 第一阶段建议优先实现接口

为了先跑通闭环，建议第一阶段优先实现以下接口：

1. 条码应用规则和批次码生成。
2. 条码解析。
3. 生产任务单创建、审核、下发、开工。
4. 工序派工生成。
5. 工序扫码和生产报工。
6. 报工反向记录冲销。
7. 产品生产状态查询。
8. 完工单生成、审核、同步。
9. 产品追溯。
10. 产量报表和不良查询。
11. 看板 WebSocket 一分钟快照推送。
