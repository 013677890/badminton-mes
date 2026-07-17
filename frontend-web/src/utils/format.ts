/** 通用格式化与小工具函数 */

/**
 * 中段脱敏：保留首尾字符，中间以 * 替代。
 * 长度不足时整体打码，避免泄露短值。
 */
export function maskText(text: string, keepStart = 3, keepEnd = 4): string {
  if (!text) return ''
  if (text.length <= keepStart + keepEnd) {
    return '*'.repeat(text.length)
  }
  const masked = '*'.repeat(Math.min(text.length - keepStart - keepEnd, 6))
  return `${text.slice(0, keepStart)}${masked}${text.slice(text.length - keepEnd)}`
}

/** 数字千分位格式化 */
export function formatNumber(value: number | string | undefined | null): string {
  if (value === undefined || value === null || value === '') return '-'
  const num = Number(value)
  if (Number.isNaN(num)) return String(value)
  return num.toLocaleString('zh-CN')
}

function pad2(value: number): string {
  return String(value).padStart(2, '0')
}

/** Date/时间戳 → 'YYYY-MM-DD HH:mm:ss'（withTime=false 时只到日期） */
export function formatDateTime(date: Date | number | string, withTime = true): string {
  const d = date instanceof Date ? date : new Date(date)
  if (Number.isNaN(d.getTime())) return String(date)
  const day = `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`
  if (!withTime) return day
  return `${day} ${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`
}

/** 简易防抖 */
export function debounce<T extends (...args: any[]) => void>(fn: T, wait = 200) {
  let timer: number | undefined
  return (...args: Parameters<T>) => {
    if (timer !== undefined) window.clearTimeout(timer)
    timer = window.setTimeout(() => fn(...args), wait)
  }
}

/**
 * 将 JSON 字符串快照格式化为缩进展示文本，解析失败降级返回原文。
 * 用于接口日志 / 异常池的 requestSnapshot 行内展开展示。
 */
export function formatSnapshot(snapshot: string | null | undefined): string {
  if (!snapshot) return '-'
  try {
    return JSON.stringify(JSON.parse(snapshot), null, 2)
  } catch {
    return snapshot
  }
}

/**
 * 解析 JSON 快照为对象，解析失败返回 null。
 * 用于异常池重试时从 requestSnapshot 预填表单。
 */
export function parseSnapshot<T = Record<string, any>>(snapshot: string | null | undefined): T | null {
  if (!snapshot) return null
  try {
    return JSON.parse(snapshot) as T
  } catch {
    return null
  }
}
