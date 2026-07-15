import { computed, ref } from 'vue'
import type { Ref } from 'vue'

export type FormDialogMode = 'create' | 'edit' | 'view'

const DEFAULT_TITLES: Record<FormDialogMode, string> = {
  create: '新增',
  edit: '编辑',
  view: '查看',
}

export interface UseFormDialogOptions<Model> {
  /** 提交处理：抛错则弹窗不关闭 */
  submit?: (model: Model, mode: FormDialogMode) => Promise<void> | void
  /** 提交成功后的回调（常用于刷新列表） */
  onSuccess?: () => void
  titles?: Partial<Record<FormDialogMode, string>>
}

/**
 * 弹窗表单逻辑复用：开关 + 模式 + 数据初始化 + 提交 loading。
 * 与 FormDialog 组件配套使用。
 */
export function useFormDialog<Model extends Record<string, any>>(
  defaults: () => Model,
  options: UseFormDialogOptions<Model> = {},
) {
  const visible = ref(false)
  const mode = ref<FormDialogMode>('create')
  const model = ref(defaults()) as Ref<Model>
  const submitLoading = ref(false)

  const title = computed(
    () => options.titles?.[mode.value] ?? DEFAULT_TITLES[mode.value],
  )
  const readonly = computed(() => mode.value === 'view')

  /** 打开弹窗：每次基于 defaults() 重建模型，避免脏数据残留 */
  function open(openMode: FormDialogMode = 'create', initial?: Partial<Model>) {
    mode.value = openMode
    model.value = { ...defaults(), ...initial }
    visible.value = true
  }

  function close() {
    visible.value = false
  }

  /** 供 FormDialog 的 submit 事件调用（校验已由组件完成） */
  async function handleSubmit() {
    if (!options.submit) {
      close()
      return
    }
    submitLoading.value = true
    try {
      await options.submit(model.value, mode.value)
      close()
      options.onSuccess?.()
    } finally {
      submitLoading.value = false
    }
  }

  return { visible, mode, model, title, readonly, submitLoading, open, close, handleSubmit }
}
