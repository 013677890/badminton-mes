import { onBeforeUnmount, onMounted, ref } from 'vue'

/**
 * 实时看板定时刷新：定时执行 + 最后更新时间 + 暂停/恢复。
 * 页面不可见时自动跳过刷新，避免后台标签页空转。
 */
export function useAutoRefresh(task: () => void | Promise<void>, intervalMs = 30000) {
  // 定时器只负责触发刷新；是否执行由暂停标志、页面可见性和并发锁共同决定。
  const lastUpdated = ref<Date | null>(null)
  const paused = ref(false)
  const refreshing = ref(false)

  let timer: number | undefined

  async function run() {
    // 防止上一次慢请求尚未完成时再次发起请求，避免看板响应乱序覆盖。
    if (refreshing.value) return
    refreshing.value = true
    try {
      await task()
      // 仅任务成功完成才更新时间，失败时保留上一次成功刷新时间。
      lastUpdated.value = new Date()
    } finally {
      refreshing.value = false
    }
  }

  function start() {
    // 重启前先清理旧 timer，保证调用 start 多次不会叠加定时器。
    stop()
    timer = window.setInterval(() => {
      if (paused.value) return
      if (document.visibilityState !== 'visible') return
      void run()
    }, intervalMs)
  }

  function stop() {
    // 组件卸载时清除浏览器定时器，避免页面销毁后仍持有回调。
    if (timer !== undefined) {
      window.clearInterval(timer)
      timer = undefined
    }
  }

  function pause() {
    // 暂停只阻止后续定时触发，不取消正在执行的请求。
    paused.value = true
  }

  function resume() {
    // 恢复时立即拉取一次，避免用户等待下一个完整周期才能看到最新数据。
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
