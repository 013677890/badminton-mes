import { ref } from 'vue'
import type { Ref } from 'vue'
import type { OptionItem } from '@/types/components'
import { get } from '@/utils/request'

interface DictEntry {
  options: Ref<OptionItem[]>
  loading: Ref<boolean>
  loaded: boolean
}

const dictCache = new Map<string, DictEntry>()

/** 静态注册的字典（演示/离线场景），命中后不再走远程加载 */
const staticDicts = new Map<string, OptionItem[]>()

/** 默认远程加载器：GET /api/dicts/{key}，可整体替换 */
let dictLoader: (key: string) => Promise<OptionItem[]> = (key) =>
  get<OptionItem[]>(`/dicts/${key}`)

export function registerDictLoader(loader: (key: string) => Promise<OptionItem[]>) {
  dictLoader = loader
}

export function registerStaticDicts(dicts: Record<string, OptionItem[]>) {
  for (const [key, options] of Object.entries(dicts)) {
    staticDicts.set(key, options)
    // 已被使用过的字典同步刷新
    const entry = dictCache.get(key)
    if (entry) {
      entry.options.value = options
      entry.loaded = true
    }
  }
}

/**
 * 字典数据加载与全局缓存：同一 key 全应用共享一份，只加载一次。
 * 用于 FilterBar / StatusTag / 下拉选项等场景。
 */
export function useDict(key: string) {
  let entry = dictCache.get(key)
  if (!entry) {
    entry = { options: ref([]), loading: ref(false), loaded: false }
    dictCache.set(key, entry)
  }
  const current = entry

  async function load(force = false) {
    if (current.loaded && !force) return
    const staticData = staticDicts.get(key)
    if (staticData) {
      current.options.value = staticData
      current.loaded = true
      return
    }
    current.loading.value = true
    try {
      current.options.value = await dictLoader(key)
      current.loaded = true
    } finally {
      current.loading.value = false
    }
  }

  void load()

  return {
    options: current.options,
    loading: current.loading,
    reload: () => load(true),
  }
}
