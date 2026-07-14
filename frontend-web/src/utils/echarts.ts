/**
 * ECharts 6 按需注册（echarts/core）。
 * 覆盖 wiki/23 图表分布：折线/柱状/饼图/仪表盘/树图/自定义系列（甘特）。
 */
import * as echarts from 'echarts/core'
import type { ComposeOption } from 'echarts/core'
import {
  BarChart,
  CustomChart,
  GaugeChart,
  LineChart,
  PieChart,
  TreeChart,
} from 'echarts/charts'
import type {
  BarSeriesOption,
  CustomSeriesOption,
  GaugeSeriesOption,
  LineSeriesOption,
  PieSeriesOption,
  TreeSeriesOption,
} from 'echarts/charts'
import {
  DataZoomComponent,
  GridComponent,
  LegendComponent,
  MarkLineComponent,
  TitleComponent,
  TooltipComponent,
} from 'echarts/components'
import type {
  DataZoomComponentOption,
  GridComponentOption,
  LegendComponentOption,
  TitleComponentOption,
  TooltipComponentOption,
} from 'echarts/components'
import { LabelLayout, UniversalTransition } from 'echarts/features'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  LineChart,
  BarChart,
  PieChart,
  GaugeChart,
  TreeChart,
  CustomChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  DataZoomComponent,
  MarkLineComponent,
  LabelLayout,
  UniversalTransition,
  CanvasRenderer,
])

export type ECOption = ComposeOption<
  | LineSeriesOption
  | BarSeriesOption
  | PieSeriesOption
  | GaugeSeriesOption
  | TreeSeriesOption
  | CustomSeriesOption
  | GridComponentOption
  | TooltipComponentOption
  | LegendComponentOption
  | TitleComponentOption
  | DataZoomComponentOption
>

export default echarts
