<script setup lang="ts" generic="Master extends Record<string, any>, Detail extends Record<string, any>">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Delete, Plus } from '@element-plus/icons-vue'
import type { DetailColumnDef } from '@/types/components'
import StatusTag from '@/components/base/StatusTag.vue'

defineOptions({ name: 'MasterDetailForm' })

const props = withDefaults(
  defineProps<{
    /** 主表单数据（页面在 #master 插槽中写 el-form-item） */
    masterModel: Master
    rules?: FormRules
    labelWidth?: string
    detailColumns: DetailColumnDef<Detail>[]
    detailTitle?: string
    /** 新增明细行工厂；缺省按列生成空行 */
    createDetailRow?: () => Detail
    addable?: boolean
    removable?: boolean
    readonly?: boolean
    /** validate 时要求的最少明细行数 */
    minDetails?: number
  }>(),
  {
    labelWidth: '100px',
    detailTitle: '明细',
    addable: true,
    removable: true,
    readonly: false,
    minDetails: 0,
  },
)

const details = defineModel<Detail[]>('details', { default: () => [] })

const emit = defineEmits<{
  'detail-add': [row: Detail]
  'detail-remove': [row: Detail, index: number]
}>()

const formRef = ref<FormInstance>()

function buildEmptyRow(): Detail {
  if (props.createDetailRow) return props.createDetailRow()
  const row: Record<string, unknown> = {}
  for (const col of props.detailColumns) {
    row[col.prop] = col.editor === 'input' ? '' : null
  }
  return row as Detail
}

function addRow() {
  const row = buildEmptyRow()
  details.value = [...details.value, row]
  emit('detail-add', row)
}

function removeRow(index: number) {
  const row = details.value[index]
  if (!row) return
  const next = [...details.value]
  next.splice(index, 1)
  details.value = next
  emit('detail-remove', row, index)
}

function displayCell(row: Detail, col: DetailColumnDef<Detail>): string {
  if (col.formatter) return col.formatter(row)
  const value = (row as Record<string, unknown>)[col.prop]
  if (value === undefined || value === null || value === '') return '-'
  return String(value)
}

/** 校验主表单 + 明细必填列 + 最少行数，通过返回 true */
async function validate(): Promise<boolean> {
  try {
    await formRef.value?.validate()
  } catch {
    return false
  }
  if (details.value.length < props.minDetails) {
    ElMessage.warning(`${props.detailTitle}至少需要 ${props.minDetails} 行`)
    return false
  }
  for (const [index, row] of details.value.entries()) {
    for (const col of props.detailColumns) {
      if (!col.required) continue
      const value = (row as Record<string, unknown>)[col.prop]
      if (value === undefined || value === null || value === '') {
        ElMessage.warning(`${props.detailTitle}第 ${index + 1} 行「${col.label}」未填写`)
        return false
      }
    }
  }
  return true
}

defineExpose({ validate, addRow, formRef })
</script>

<template>
  <el-form
    ref="formRef"
    :model="masterModel"
    :rules="rules"
    :label-width="labelWidth"
    :disabled="readonly"
    class="mdf"
  >
    <slot name="master" />

    <el-divider content-position="left">{{ detailTitle }}</el-divider>

    <div v-if="(addable && !readonly) || $slots['detail-toolbar']" class="mdf__toolbar">
      <el-button v-if="addable && !readonly" type="primary" plain :icon="Plus" @click="addRow">
        新增行
      </el-button>
      <slot name="detail-toolbar" />
    </div>

    <el-table :data="details" border>
      <el-table-column type="index" label="#" width="50" align="center" />
      <el-table-column
        v-for="col in detailColumns"
        :key="col.prop"
        :label="col.label"
        :width="col.width"
        :min-width="col.minWidth ?? 140"
        :align="col.align"
      >
        <template v-if="col.required" #header>
          <span class="mdf__required">*</span>
          {{ col.label }}
        </template>
        <template #default="scope">
          <slot
            :name="`detail-row-${col.prop}`"
            :row="scope.row as Detail"
            :index="scope.$index"
          >
            <el-input
              v-if="col.editor === 'input' && !readonly"
              v-model="scope.row[col.prop]"
              :placeholder="col.placeholder ?? '请输入'"
            />
            <el-input-number
              v-else-if="col.editor === 'number' && !readonly"
              v-model="scope.row[col.prop]"
              :min="0"
              controls-position="right"
              class="mdf__number"
            />
            <el-select
              v-else-if="col.editor === 'select' && !readonly"
              v-model="scope.row[col.prop]"
              :placeholder="col.placeholder ?? '请选择'"
              clearable
            >
              <el-option
                v-for="opt in col.options ?? []"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
                :disabled="opt.disabled"
              />
            </el-select>
            <el-date-picker
              v-else-if="col.editor === 'date' && !readonly"
              v-model="scope.row[col.prop]"
              type="date"
              value-format="YYYY-MM-DD"
              :placeholder="col.placeholder ?? '选择日期'"
              class="mdf__date"
            />
            <StatusTag
              v-else-if="col.statusMap"
              :status="scope.row[col.prop]"
              :status-map="col.statusMap"
            />
            <template v-else>{{ displayCell(scope.row, col) }}</template>
          </slot>
        </template>
      </el-table-column>
      <el-table-column v-if="removable && !readonly" label="操作" width="70" align="center">
        <template #default="scope">
          <el-button
            link
            type="danger"
            :icon="Delete"
            @click="removeRow(scope.$index)"
          />
        </template>
      </el-table-column>
      <template #empty>
        <span class="mdf__empty">暂无{{ detailTitle }}{{ addable && !readonly ? '，点击"新增行"添加' : '' }}</span>
      </template>
    </el-table>
  </el-form>
</template>

<style scoped>
.mdf__toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.mdf__required {
  margin-right: 2px;
  color: var(--el-color-danger);
}

.mdf__number,
.mdf__date {
  width: 100%;
}

.mdf__empty {
  color: var(--el-text-color-secondary);
}
</style>
