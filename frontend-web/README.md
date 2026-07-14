# 羽毛球 MES 管理后台（frontend-web）

Vue 3.5 + Vite 8 + TypeScript + Element Plus + ECharts 6 + Pinia + Vue Router。

通用组件库对应 [wiki/23-前端组件设计规划](../wiki/23-前端组件设计规划.md)，按"配置驱动 + 插槽扩展 + 组合优于继承"实现。

## 启动

```bash
npm install
npm run dev      # 开发（/api 代理到 http://localhost:8080）
npm run build    # vue-tsc 类型检查 + 生产构建
```

登录页为本地模拟登录（可勾选演示角色，影响菜单与按钮权限），接入认证模块后替换 `stores/user.ts` 的 `login()`。

## 目录结构

```text
src/
├── layouts/                  # BasicLayout（PC 框架）/ TabletLayout（平板）/ BlankLayout
├── components/
│   ├── base/                 # 基础原子组件（业务无关薄封装）
│   │   └── charts/           # ChartWrapper + Line/Bar/Pie/Gauge 卡片
│   └── business/             # 业务通用组件（schema 驱动厚封装）
│       └── touch/            # 平板触摸优化组件
├── composables/              # useTable / useFormDialog / usePermission / useScan / useChart / useAutoRefresh / useDict
├── views/                    # 页面（demo/* 为组件示例页，可直接照抄组装真实页面）
├── router/                   # 路由（menuRoutes 树 → 拍平注册，meta.roles 控权限）
├── stores/                   # user（登录态/角色）、app（侧边栏/标签页/keep-alive）
├── types/components.ts       # FilterField / ColumnDef / StatusMap 等全部共享 schema 类型
├── utils/                    # request（对齐后端 CommonResult/PageResult）、echarts 按需注册、format
└── styles/                   # 全局样式变量
```

## 组件清单

### 基础组件 components/base/

| 组件 | 说明 |
| --- | --- |
| PageHeader | 页头：标题 + 描述 + `#extra` 操作区，可带返回 |
| FilterBar | schema 配置筛选区：`fields: FilterField[]`，展开/收起，`query/reset` 事件只吐纯参数 |
| ProTable | `columns: ColumnDef[]` schema 表格 + 分页 + 操作列；单元格优先级 `#col-{prop}` 插槽 > statusMap > formatter > 原值 |
| FormDialog | 弹窗 + 表单校验 + 确认/取消；`v-model:visible`，内容超高滚动 |
| DescList | 详情描述列表：`items: DescItem[]`，支持脱敏 `mask`、statusMap、formatter |
| StatusTag | 状态码 → 颜色/文案映射，未映射降级原样展示 |
| StatCard | 指标卡：数值 + 标题 + 趋势 + 图标 |
| FileUploader | 上传封装：类型/大小/数量校验，缺省不自动上传 |
| PermissionButton | 按 `roles` 显隐或置灰（配合 usePermission） |
| EmptyState | 空数据 / 数据暂不可用 |

### 业务组件 components/business/

| 组件 | 复用场景 |
| --- | --- |
| FilterTable | 几乎所有列表页：FilterBar + ProTable 双 schema 组合，插槽全透传 |
| MasterDetailForm | 主表单 + 可增删行内编辑明细（工单+物料、报工+不良、检验单+项目…） |
| StatusTimeline | 工单日志/产品履历/设备履历时间轴 |
| ApprovalActionBar | 通过/驳回 + 审核意见，按角色显隐 |
| ScanInput | 扫码输入：回车提交、防重复、校验反馈（配合 useScan 支持全局扫码枪捕获） |
| TreeManager | 类别树管理：搜索 + 行内操作 + 右键菜单 |
| StatusCardGrid | 状态卡片网格（齐套看板/生产实时/设备状态） |
| BatchToolbar | 已选统计 + 批量操作 + 二次确认 |
| QueryChartPanel | 查询区 + 图表 + 明细表三段式报表页 |
| ConfigForm | 分组配置表单 + 变更日志 |
| TabDetailPage | Tab 详情页壳，懒渲染 |
| ImportExport | Excel 导入（模板下载/校验反馈）+ 导出 |
| GanttSchedule | 派工排程甘特（ECharts custom series） |
| TraceLinkGraph | 正/反向追溯链路树图 |
| touch/Touch* | TouchCardList / TouchActionButtons / TouchSimpleForm / TouchSopViewer / TouchScanPage |

### 组合式函数 composables/

`useTable`（分页列表全流程，与 FilterTable 事件一一对应）、`useFormDialog`（弹窗开关/模式/提交）、`usePermission`（hasRole）、`useScan`（扫码防抖+全局捕获）、`useChart`（ECharts 生命周期/resize/空容器重试）、`useAutoRefresh`（看板定时刷新）、`useDict`（字典缓存，可注册静态字典或替换加载器）。

## 约定

- 页面组件 `defineOptions({ name })` 必须与路由 `name` 一致，否则 keep-alive 标签页缓存不生效。
- 新增菜单在 `router/routes.ts` 的 `menuRoutes` 树上加节点（`meta.title/icon/roles`），会自动进侧边菜单/面包屑/标签页。
- 列表页三件套：`filterFields` + `columns` 两个 schema + `useTable(fetcher)`，模板只写 `<FilterTable>` 与个性化插槽。
- 后端契约：`CommonResult{code:'00000',...}` 在 `utils/request.ts` 拦截器统一解包；分页对齐 `PageParam/PageResult(pageNo/pageSize/list/total)`。
