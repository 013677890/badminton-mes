import { onBeforeUnmount, onMounted, ref } from 'vue'

/**
 * 实时看板定时刷新：定时执行 + 最后更新时间 + 暂停/恢复。
 * 页面不可见时自动跳过刷新，避免后台标签页空转。
 */
export function useAutoRefresh(task: () => void | Promise<void>, intervalMs = 30000) {
  const lastUpdated = ref<Date | null>(null)
  const paused = ref(false)
  const refreshing = ref(false)

  let timer: number | undefined

  async function run() {
    if (refreshing.value) return
    refreshing.value = true
    try {
      await task()
      lastUpdated.value = new Date()
    } finally {
      refreshing.value = false
    }
  }

  function start() {
    stop()
    timer = window.setInterval(() => {
      if (paused.value) return
      if (document.visibilityState !== 'visible') return
      void run()
    }, intervalMs)
  }

  function stop() {
    if (timer !== undefined) {
      window.clearInterval(timer)
      timer = undefined
    }
  }

  function pause() {
    paused.value = true
  }

  function resume() {
    paused.value = false
    void run()
  }

  onMounted(() => {
    void run()
    start()
  })

  onBeforeUnmount(stop)

  return { lastUpdated, paused, refreshing, pause, resume, refreshNow: run }
}
