<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ArrowDown, ArrowUp, Refresh, Search } from '@element-plus/icons-vue'
import type { CascaderOption } from 'element-plus'
import type { FilterField } from '@/types/components'

defineOptions({ name: 'FilterBar' })

const props = withDefaults(
  defineProps<{
    fields: FilterField[]
    /** 收起时显示的字段数，0 表示不折叠，默认 3 */
    collapsedCount?: number
    labelWidth?: string
  }>(),
  { collapsedCount: 3, labelWidth: '80px' },
)

const emit = defineEmits<{
  /** 筛选栏仅负责收集原始筛选值，具体页面再转换为各自的请求参数类型。 */
  query: [params: any]
  reset: [params: any]
}>()

function defaultFor(field: FilterField): unknown {
  if (field.defaultValue !== undefined) return field.defaultValue
  if (field.type === 'select' && field.multiple) return []
  return null
}

/** 筛选值集中在内部 model，页面只通过 query/reset 事件拿纯数据 */
const model = reactive<Record<string, any>>({})

function initDefaults() {
  for (const field of props.fields) {
    model[field.prop] = defaultFor(field)
  }
}

initDefaults()

const expanded = ref(false)
const collapsible = computed(
  () => props.collapsedCount > 0 && props.fields.length > props.collapsedCount,
)
const visibleFields = computed(() =>
  !collapsible.value || expanded.value
    ? props.fields
    : props.fields.slice(0, props.collapsedCount),
)

/** 操作按钮列补满当前行剩余栅格并右对齐 */
const actionsSpan = computed(() => {
  const used = visibleFields.value.reduce((sum, field) => sum + (field.span ?? 6), 0)
  const remain = 24 - (used % 24)
  return remain >= 6 ? remain : 24
})

/** 过滤空值（保留 false 与 0），空数组视为未选择 */
function buildParams(): Record<string, any> {
  const params: Record<string, any> = {}
  for (const field of props.fields) {
    const value = model[field.prop]
    if (value === undefined || value === null || value === '') continue
    if (Array.isArray(value) && value.length === 0) continue
    params[field.prop] = value
  }
  return params
}

function handleQuery() {
  emit('query', buildParams())
}

/** OptionItem 与 CascaderOption 结构兼容，仅缺索引签名 */
function asCascaderOptions(field: FilterField): CascaderOption[] {
  return (field.options ?? []) as unknown as CascaderOption[]
}

function handleReset() {
  initDefaults()
  emit('reset', buildParams())
}

defineExpose({
  /** 获取当前筛选参数（已过滤空值） */
  getParams: buildParams,
  /** 外部回填筛选值（如从路由参数恢复） */
  setParams: (params: Record<string, any>) => Object.assign(model, params),
})
</script>

<template>
  <el-form class="filter-bar" :label-width="labelWidth" @submit.prevent="handleQuery">
    <el-row :gutter="16">
      <el-col v-for="field in visibleFields" :key="field.prop" :span="field.span ?? 6">
        <el-form-item :label="field.label">
          <el-input
            v-if="field.type === 'input'"
            v-model="model[field.prop]"
            :placeholder="field.placeholder ?? `请输入${field.label}`"
            clearable
            @keyup.enter="handleQuery"
          />
          <el-select
            v-else-if="field.type === 'select'"
            v-model="model[field.prop]"
            :placeholder="field.placeholder ?? `请选择${field.label}`"
            :multiple="field.multiple"
            clearable
            class="filter-bar__control"
          >
            <el-option
              v-for="opt in field.options ?? []"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
              :disabled="opt.disabled"
            />
          </el-select>
          <el-date-picker
            v-else-if="field.type === 'date'"
            v-model="model[field.prop]"
            type="date"
            value-format="YYYY-MM-DD"
            :placeholder="field.placeholder ?? `请选择${field.label}`"
            clearable
            class="filter-bar__control"
          />
          <el-date-picker
            v-else-if="field.type === 'dateRange'"
            v-model="model[field.prop]"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            clearable
            class="filter-bar__control"
          />
          <el-cascader
            v-else-if="field.type === 'cascader'"
            v-model="model[field.prop]"
            :options="asCascaderOptions(field)"
            :placeholder="field.placeholder ?? `请选择${field.label}`"
            clearable
            class="filter-bar__control"
          />
        </el-form-item>
      </el-col>
      <el-col :span="actionsSpan" class="filter-bar__actions">
        <el-form-item label-width="0">
          <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          <el-button v-if="collapsible" link type="primary" @click="expanded = !expanded">
            {{ expanded ? '收起' : '展开' }}
            <el-icon>
              <ArrowUp v-if="expanded" />
              <ArrowDown v-else />
            </el-icon>
          </el-button>
          <slot name="extra" />
        </el-form-item>
      </el-col>
    </el-row>
  </el-form>
</template>

<style scoped>
.filter-bar :deep(.el-form-item) {
  margin-bottom: 12px;
}

.filter-bar__control {
  width: 100%;
}

.filter-bar__actions :deep(.el-form-item__content) {
  justify-content: flex-end;
}
</style>
