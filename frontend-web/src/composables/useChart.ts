import { onBeforeUnmount, onMounted } from 'vue'
import type { Ref } from 'vue'
import echarts from '@/utils/echarts'
import type { ECOption } from '@/utils/echarts'
import { debounce } from '@/utils/format'

type EChartsInstance = ReturnType<typeof echarts.init>

/** 容器不可见（Tab/弹窗未展开）时的重试间隔与上限 */
const RETRY_INTERVAL_MS = 50
const MAX_RETRY = 40

/**
 * ECharts 实例生命周期管理：初始化 / resize / 销毁。
 * - 容器宽高为 0 时延迟重试，规避 Tab、Dialog 首渲失败
 * - ResizeObserver 监听容器尺寸（侧边栏折叠也能触发），200ms 防抖
 * - setOption 默认先 clear，防止旧 series 残留
 */
export function useChart(elRef: Ref<HTMLElement | undefined>) {
  let chart: EChartsInstance | null = null
  let observer: ResizeObserver | null = null
  let cachedOption: ECOption | null = null
  let retryCount = 0
  let retryTimer: number | undefined

  function ensureInit(): boolean {
    if (chart) return true
    const el = elRef.value
    if (!el) return false
    if (el.offsetHeight === 0 || el.offsetWidth === 0) return false
    chart = echarts.init(el)
    return true
  }

  function setOption(option: ECOption, clear = true) {
    cachedOption = option
    if (!ensureInit()) {
      // 容器尚不可见：限次重试
      if (retryTimer !== undefined) window.clearTimeout(retryTimer)
      if (retryCount >= MAX_RETRY) return
      retryCount += 1
      retryTimer = window.setTimeout(() => setOption(option, clear), RETRY_INTERVAL_MS)
      return
    }
    retryCount = 0
    if (clear) chart!.clear()
    chart!.setOption(option)
  }

  function resize() {
    chart?.resize({ animation: { duration: 300 } })
  }

  const debouncedResize = debounce(resize, 200)

  function setLoading(value: boolean) {
    if (!chart) return
    if (value) {
      chart.showLoading({ text: '加载中...', maskColor: 'rgba(255,255,255,0.6)' })
    } else {
      chart.hideLoading()
    }
  }

  /** 绑定图表事件（如 click 下钻）；实例未就绪时挂到初始化之后 */
  function onEvent(event: string, handler: (params: unknown) => void) {
    if (ensureInit()) {
      chart!.on(event, handler)
    } else {
      const timer = window.setInterval(() => {
        if (ensureInit()) {
          window.clearInterval(timer)
          chart!.on(event, handler)
        }
      }, RETRY_INTERVAL_MS)
      window.setTimeout(() => window.clearInterval(timer), RETRY_INTERVAL_MS * MAX_RETRY)
    }
  }

  onMounted(() => {
    ensureInit()
    if (cachedOption && chart) setOption(cachedOption)
    if (elRef.value) {
      observer = new ResizeObserver(() => debouncedResize())
      observer.observe(elRef.value)
    }
  })

  onBeforeUnmount(() => {
    if (retryTimer !== undefined) window.clearTimeout(retryTimer)
    observer?.disconnect()
    observer = null
    chart?.dispose()
    chart = null
  })

  return {
    setOption,
    resize,
    setLoading,
    onEvent,
    getInstance: () => chart,
  }
}
