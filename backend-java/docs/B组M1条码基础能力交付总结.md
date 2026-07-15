# B组 M1 条码基础能力交付总结

> 作者：刘涵　日期：2026-07-12　分支：PartB
>
> 本文是 M1 里程碑（条码基础能力）的一次性交付总结，权威口径以
> 《B组后端项目总体实施规划.md》第 9/21/25 节为准。

## 1. 交付范围

按规划文档薄切片顺序，M1 全部 8 个切片已实现并通过测试：

| 切片 | 内容 | 端点数 |
| --- | --- | --- |
| 条码类型 | CRUD / 启停 / 选项，被规则引用不可删 | 8 |
| 条码规则 + 明细 | 常量/日期/变量/流水号四段组合，preview / validate | 9 |
| 条码模板 + 字段 | 未绑定就地改，被应用规则绑定后自动升版本保留历史 | 7 |
| 应用规则 | 对象匹配、启用前三档案校验、默认规则唯一（预检 + 生成列兜底） | 8 |
| 条码实例 | generate / batch_generate / parse / cancel / 详情 / 分页 | — |
| 打印记录 | 逐次插入模型，重复打印必填原因，模板版本 + 预览 JSON 快照 | — |
| 使用记录 | 扫码使用留痕，按业务时间倒序查询 | — |
| 外部导入 | JSON 数组同步导入，单次上限 500，部分成功、逐条失败原因 | — |

代码位于 `backend-java/src/main/java/com/badminton/mes/module/barcode/`
（100 个主文件），测试位于对应 test 目录（12 个测试类）。全部
`@author 刘涵`。

## 2. 已冻结的业务决策（本次落地）

1. **流水周期与格式上限**（M1 待确认事项①）：重置周期 1按日 / 2按月 /
   3不重置；`serial_length` 上限 9 位，溢出报"规则容量不足"不回绕；
   条码值总长 ≤64；流水作用域 ≤64。
2. **条码导入口径**（M1 待确认事项②）：JSON 数组同步导入，前端解析文件
   后提交；单次最多 500 条，超限整单拒绝；逐条校验格式、长度与重复性，
   响应返回逐条失败原因（部分成功模型）。
3. 沿用既冻结决策：一批一码；Redis INCR 发号 + MySQL 唯一索引兜底；
   不驱动真实打印机（仅记录 + 预览快照）；作废走冲销记录、已使用不可作废。

## 3. 关键实现要点

- **发号与查重**：Redis INCR 发流水（Key 丢失时从 `barcode_serial.current_serial`
  播种恢复）→ `BarcodeValueComposer` 组合条码值 → 应用层预检查重 + 重试
  （上限后报 `BARCODE_GENERATE_CONFLICT`）→ 落库由 `uk_barcode_value`
  唯一索引兜底（捕获 `DataIntegrityViolationException`）→ 单调推进
  MySQL 流水事实（`advanceSerial` 只前推，容忍乱序提交）。
- **状态机**：条码状态 0未使用 / 1已使用 / 2已作废；"已使用不可作废"由
  CAS 更新（`updateStatus(id, fromStatus, toStatus)`）原子保证。
- **打印**：`print_count = max + 1`，`(barcode_id, print_count)` 唯一；
  `printCount > 1` 必填补打原因；预览内容序列化为 JSON 快照持久化。
- **数据权限**：建 `base_product` / `base_material` / `prod_work_order`
  的 `@Immutable` 只读引用实体；非管理员按所属车间收敛工单可见性，
  越权与不存在统一按"无可见数据"处理。
- **角色契约**：生成/作废/导入限 ADMIN + PMC；打印另放开 TEAM_LEADER；
  解析/查询登录即可。契约由 Controller 反射测试锁定。

## 4. 数据库变更

- 迁移脚本：`V2026071201__add_b_group_barcode_schema.sql`（Flyway 全局
  编号 VyyyyMMddNN），建 9 张 `barcode_*` 表 + `barcode_use_record`。
- 变更登记：`badminton-mes.wiki/database/changes/2026-07-12-B组M1条码结构迁移.md`。
- 落实 2026-07-11 已登记的三处修正：`barcode_serial` 全列唯一索引、
  `barcode_apply_rule` 生成列唯一约束（同对象同类型仅一条启用默认规则）、
  打印记录逐次插入 + 模板版本/预览快照字段。

## 5. 验证记录

- `Z:\gradlew.bat -p Z:\ clean compileJava test --no-daemon`
  → BUILD SUCCESSFUL，**281 个测试，0 失败**（含条码模块 12 个测试类）。
- Windows 中文路径坑：Gradle test worker 报 ClassNotFoundException，
  统一用 `subst Z: <项目路径>` 映射 ASCII 盘符后执行。

## 6. 遗留事项（非本次引入）

- 空库 Flyway 全量迁移、`ddl-auto=validate` 启动、真库唯一索引/生成列
  行为验证，仍受 M0 登记的环境阻塞（本机无 Docker、无 MySQL 8.4 验证
  账号），环境可用后按 wiki 变更说明第 6 节补验。
- 下一里程碑为 M2 现场执行（生产参数/任务/派工），开工前需确认 A 组
  工艺路线迁移进展。
