import { onBeforeUnmount, onMounted, ref } from 'vue'

export interface UseScanOptions {
  /** 扫码结果处理，抛错视为处理失败 */
  onScan: (code: string) => void | Promise<void>
  /** 最小码长，低于此长度忽略，默认 4 */
  minLength?: number
  /** 同一条码冷却时间（ms），期间重复扫码忽略，默认 800 */
  cooldownMs?: number
  /**
   * 全局捕获模式：监听 document 键盘输入（无输入框场景，如平板全屏扫码页）。
   * 扫码枪击键间隔极短，超过 gapMs 视为人工输入并丢弃缓冲。
   */
  global?: boolean
  /** 全局模式下判定为扫码枪连续输入的最大击键间隔（ms），默认 50 */
  gapMs?: number
}

/**
 * 扫码逻辑复用：防重复提交 + 扫码枪键盘模拟识别。
 * ScanInput 组件覆盖常规输入框场景；本函数供全局捕获/自定义 UI 场景使用。
 */
export function useScan(options: UseScanOptions) {
  const { onScan, minLength = 4, cooldownMs = 800, global = false, gapMs = 50 } = options

  const scanning = ref(false)
  const lastCode = ref('')

  let cooldownCode = ''
  let cooldownAt = 0

  async function submit(raw: string) {
    const code = raw.trim()
    if (code.length < minLength) return
    const now = Date.now()
    if (code === cooldownCode && now - cooldownAt < cooldownMs) return
    cooldownCode = code
    cooldownAt = now
    scanning.value = true
    try {
      await onScan(code)
      lastCode.value = code
    } finally {
      scanning.value = false
    }
  }

  // ---- 全局键盘捕获（扫码枪模拟键盘） ----
  let buffer = ''
  let lastKeyAt = 0

  function handleKeydown(event: KeyboardEvent) {
    // 焦点在输入框时交给输入框自身处理，避免重复触发
    const target = event.target as HTMLElement | null
    if (target && ['INPUT', 'TEXTAREA'].includes(target.tagName)) return

    const now = Date.now()
    if (now - lastKeyAt > gapMs) buffer = ''
    lastKeyAt = now

    if (event.key === 'Enter') {
      if (buffer) void submit(buffer)
      buffer = ''
      return
    }
    if (event.key.length === 1) buffer += event.key
  }

  if (global) {
    onMounted(() => document.addEventListener('keydown', handleKeydown))
    onBeforeUnmount(() => document.removeEventListener('keydown', handleKeydown))
  }

  return { scanning, lastCode, submit }
}
