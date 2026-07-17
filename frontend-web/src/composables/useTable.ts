import { computed, ref } from 'vue'
import type { Ref } from 'vue'
import type { Pagination } from '@/types/components'
import type { PageParam, PageResult } from '@/utils/request'

export interface UseTableOptions<Row, Params extends Record<string, any>> {
  /** 分页查询函数：接收 筛选参数 + 分页参数，返回 PageResult */
  fetcher: (params: Params & PageParam) => Promise<PageResult<Row>>
  defaultParams?: Partial<Params>
  defaultPageSize?: number
  /** 创建后立即加载，默认 true */
  immediate?: boolean
}

/**
 * 分页列表逻辑复用：请求/筛选/重置/分页/loading。
 * 与 FilterTable 的 query / reset / page-change 事件一一对应。
 */
export function useTable<Row, Params extends Record<string, any> = Record<string, any>>(
  options: UseTableOptions<Row, Params>,
) {
  const { fetcher, defaultParams, defaultPageSize = 10, immediate = true } = options

  const data = ref<Row[]>([]) as Ref<Row[]>
  const loading = ref(false)
  const pagination = ref<Pagination>({ pageNo: 1, pageSize: defaultPageSize, total: 0 })

  /** 当前生效的筛选条件（查询/翻页共用） */
  let activeParams: Record<string, any> = { ...defaultParams }

  const searchParams = computed(() => ({ ...activeParams }))

  /**
   * 按当前筛选条件和分页状态加载列表，并同步 loading、数据和总条数。
   */
  async function load() {
    loading.value = true
    try {
      const result = await fetcher({
        ...(activeParams as Params),
        pageNo: pagination.value.pageNo,
        pageSize: pagination.value.pageSize,
      })
      data.value = result.list
      pagination.value.total = result.total
    } finally {
      loading.value = false
    }
  }

  /** 新条件查询：回到第 1 页 */
  /**
   * 使用新筛选条件查询；查询条件变化时必须回到第一页。
   */
  function query(params?: Params) {
    if (params) activeParams = { ...params }
    pagination.value.pageNo = 1
    return load()
  }

  /** 重置：恢复默认条件并回到第 1 页 */
  /**
   * 恢复初始化筛选条件，并重新加载第一页数据。
   */
  function reset() {
    activeParams = { ...defaultParams }
    pagination.value.pageNo = 1
    return load()
  }

  /** 保持当前条件与页码刷新（增删改后局部刷新用） */
  /**
   * 保留当前条件和页码刷新列表，适用于增删改操作完成后更新页面。
   */
  function refresh() {
    return load()
  }

  /**
   * 接收表格分页事件，更新分页状态后重新请求列表。
   */
  function onPageChange(page: { pageNo: number; pageSize: number }) {
    pagination.value.pageNo = page.pageNo
    pagination.value.pageSize = page.pageSize
    return load()
  }

  if (immediate) {
    void load()
  }

  return { data, loading, pagination, searchParams, query, reset, refresh, onPageChange }
}
