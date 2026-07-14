/**
 * 组件库共享类型定义
 *
 * 对应 wiki/23-前端组件设计规划.md 的 schema 约定：
 * 表格列、筛选字段、状态映射等均通过这里的类型配置驱动。
 */

// ---------- 通用 ----------

/** Element Plus 标签/按钮语义色 */
export type TagType = 'primary' | 'success' | 'info' | 'warning' | 'danger'

export interface StatusMeta {
  type: TagType
  text: string
}

/** 状态码 → 颜色 + 文案映射，如 { DRAFT: { type: 'info', text: '草稿' } } */
export type StatusMap = Record<string, StatusMeta>

export interface OptionItem {
  label: string
  value: string | number
  disabled?: boolean
  /** cascader 级联子项 */
  children?: OptionItem[]
}

// ---------- FilterBar ----------

export type FilterFieldType = 'input' | 'select' | 'date' | 'dateRange' | 'cascader'

/** 筛选字段配置 */
export interface FilterField {
  prop: string
  label: string
  type: FilterFieldType
  options?: OptionItem[]
  defaultValue?: unknown
  /** 24 栅格制占位，默认 6 */
  span?: number
  placeholder?: string
  /** select 多选 */
  multiple?: boolean
}

// ---------- ProTable ----------

/** 表格列定义（映射 ElTableColumn 属性 + MES 扩展） */
export interface ColumnDef<Row extends Record<string, any> = Record<string, any>> {
  prop: string
  label: string
  width?: number | string
  minWidth?: number | string
  fixed?: 'left' | 'right'
  align?: 'left' | 'center' | 'right'
  sortable?: boolean | 'custom'
  showOverflowTooltip?: boolean
  /** 配置后该列用 StatusTag 渲染，单元格值作为 status */
  statusMap?: StatusMap
  /** 列内容格式化；优先级：#col-{prop} 插槽 > statusMap > formatter > 原值 */
  formatter?: (row: Row) => string
}

/** 分页模型，与后端 PageParam / PageResult 字段对齐 */
export interface Pagination {
  pageNo: number
  pageSize: number
  total: number
}

/** 配置驱动的行操作按钮 */
export interface RowAction<Row extends Record<string, any> = Record<string, any>> {
  key: string
  label: string
  type?: TagType
  /** 需要的角色编码，不配则所有人可见 */
  roles?: string[]
  /** 配置后点击先弹确认气泡，值为确认文案 */
  confirm?: string
  show?: (row: Row) => boolean
  disabled?: (row: Row) => boolean
}

// ---------- MasterDetailForm ----------

export type DetailEditorType = 'text' | 'input' | 'number' | 'select' | 'date'

/** 主从表单的明细列：在表格列基础上增加行内编辑配置 */
export interface DetailColumnDef<Row extends Record<string, any> = Record<string, any>>
  extends ColumnDef<Row> {
  /** 行内编辑控件类型，text/缺省 为只读展示 */
  editor?: DetailEditorType
  options?: OptionItem[]
  required?: boolean
  placeholder?: string
}

// ---------- DescList ----------

export interface DescItem<Row extends Record<string, any> = Record<string, any>> {
  prop: string
  label: string
  span?: number
  /** 敏感信息脱敏展示 */
  mask?: boolean
  statusMap?: StatusMap
  formatter?: (data: Row) => string
}

// ---------- StatusTimeline ----------

export interface StatusNode {
  status: string
  time: string
  operator?: string
  remark?: string
}

// ---------- StatusCardGrid ----------

export interface StatusCardMetric {
  label: string
  value: string | number
  unit?: string
}

export interface StatusCardItem {
  key: string | number
  title: string
  subtitle?: string
  /** 与 statusMap 配合决定卡片状态色 */
  status?: string
  metrics?: StatusCardMetric[]
}

// ---------- BatchToolbar ----------

export interface BatchAction {
  key: string
  label: string
  type?: TagType
  /** 配置后执行前弹二次确认，值为确认文案 */
  confirm?: string
  roles?: string[]
}

// ---------- ConfigForm ----------

export type ConfigItemType = 'input' | 'number' | 'switch' | 'select' | 'textarea'

export interface ConfigItem {
  key: string
  label: string
  type: ConfigItemType
  options?: OptionItem[]
  /** 配置项说明，展示为 label 旁问号提示 */
  tip?: string
  /** 24 栅格制占位，默认 12 */
  span?: number
  required?: boolean
  placeholder?: string
}

export interface ConfigGroup {
  title: string
  items: ConfigItem[]
}

export interface ConfigChangeLog {
  time: string
  operator: string
  content: string
}

// ---------- TabDetailPage ----------

export interface TabItem {
  name: string
  label: string
  /** 首次激活才渲染（懒加载），默认 true */
  lazy?: boolean
}

// ---------- TreeManager ----------

export interface TreeNodeData {
  id: string | number
  label: string
  disabled?: boolean
  children?: TreeNodeData[]
  [key: string]: unknown
}

export type TreeOperation = 'add' | 'edit' | 'disable' | 'delete'

// ---------- ImportExport ----------

export interface ImportError {
  row: number
  message: string
}

export interface ImportResult {
  successCount: number
  failCount: number
  errors: ImportError[]
}

// ---------- 图表 ----------

export interface ChartSeriesItem {
  name: string
  data: number[]
  /** 柱状图堆叠分组名 */
  stack?: string
}

/** 折线/柱状等直角坐标图数据 */
export interface AxisChartData {
  categories: (string | number)[]
  series: ChartSeriesItem[]
}

export interface PieDataItem {
  name: string
  value: number
}

export type QueryChartType = 'line' | 'bar' | 'pie'

// ---------- GanttSchedule ----------

export interface GanttRow {
  key: string
  label: string
}

export interface GanttTask {
  id: string | number
  /** 所属产线/设备行，对应 GanttRow.key */
  rowKey: string
  name: string
  /** 时间字符串，如 '2026-07-14 08:00' */
  start: string
  end: string
  status?: string
}

// ---------- TraceLinkGraph ----------

export interface TraceNode {
  id: string
  name: string
  /** 节点类型（material/process/quality...），用于配色 */
  type?: string
  meta?: Record<string, string | number>
  children?: TraceNode[]
}

// ---------- 平板触摸组件 ----------

export interface TouchCardField {
  label: string
  value: string | number
}

export interface TouchCardItem {
  key: string | number
  title: string
  subtitle?: string
  status?: string
  fields?: TouchCardField[]
  disabled?: boolean
}

export interface TouchActionItem {
  key: string
  label: string
  type?: TagType
  disabled?: boolean
}

export interface SopMediaItem {
  type: 'image' | 'video' | 'doc'
  url: string
  title?: string
}

export type TouchFieldType = 'text' | 'number' | 'select'

export interface TouchFormField {
  prop: string
  label: string
  type: TouchFieldType
  options?: OptionItem[]
  placeholder?: string
  required?: boolean
}
