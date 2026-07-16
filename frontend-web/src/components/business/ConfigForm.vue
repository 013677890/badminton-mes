<script setup lang="ts">
import { computed, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { QuestionFilled } from '@element-plus/icons-vue'
import type { ConfigChangeLog, ConfigGroup, ConfigItem } from '@/types/components'

defineOptions({ name: 'ConfigForm' })

const props = withDefaults(
  defineProps<{
    groups: ConfigGroup[]
    labelWidth?: string
    saveLoading?: boolean
    /** 配置变更日志，传入后底部折叠面板展示 */
    logs?: ConfigChangeLog[]
  }>(),
  { labelWidth: '140px', saveLoading: false },
)

const model = defineModel<Record<string, any>>({ required: true })

const emit = defineEmits<{
  save: [model: Record<string, any>]
  reset: []
}>()

const formRef = ref<FormInstance>()

function requiredMessage(item: ConfigItem): string {
  const verb = item.type === 'select' || item.type === 'switch' ? '请选择' : '请输入'
  return `${verb}${item.label}`
}

/** required 配置项自动生成必填规则，页面无需重复声明 */
const rules = computed<FormRules>(() => {
  const result: FormRules = {}
  for (const group of props.groups) {
    for (const item of group.items) {
      if (!item.required) continue
      result[item.key] = [
        { required: true, message: requiredMessage(item), trigger: ['blur', 'change'] },
      ]
    }
  }
  return result
})

async function validate(): Promise<boolean> {
  try {
    await formRef.value?.validate()
    return true
  } catch {
    return false
  }
}

async function handleSave() {
  if (!(await validate())) return
  emit('save', model.value)
}

/** 数据恢复交给父层（只有父层知道初始值），组件只清校验态 */
function handleReset() {
  formRef.value?.clearValidate()
  emit('reset')
}

defineExpose({ validate })
</script>

<template>
  <div class="config-form">
    <el-form ref="formRef" :model="model" :rules="rules" :label-width="labelWidth">
      <template v-for="group in groups" :key="group.title">
        <el-divider content-position="left">{{ group.title }}</el-divider>
        <el-row :gutter="16">
          <el-col v-for="item in group.items" :key="item.key" :span="item.span ?? 12">
            <el-form-item :prop="item.key" :label="item.label">
              <template #label>
                <span class="config-form__label">
                  {{ item.label }}
                  <el-tooltip v-if="item.tip" :content="item.tip" placement="top">
                    <el-icon class="config-form__tip"><QuestionFilled /></el-icon>
                  </el-tooltip>
                </span>
              </template>
              <el-input
                v-if="item.type === 'input'"
                v-model="model[item.key]"
                :placeholder="item.placeholder ?? '请输入'"
                clearable
              />
              <el-input-number
                v-else-if="item.type === 'number'"
                v-model="model[item.key]"
                controls-position="right"
                :placeholder="item.placeholder"
                class="config-form__number"
              />
              <el-switch v-else-if="item.type === 'switch'" v-model="model[item.key]" />
              <el-select
                v-else-if="item.type === 'select'"
                v-model="model[item.key]"
                :placeholder="item.placeholder ?? '请选择'"
                clearable
              >
                <el-option
                  v-for="opt in item.options ?? []"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                  :disabled="opt.disabled"
                />
              </el-select>
              <el-input
                v-else-if="item.type === 'textarea'"
                v-model="model[item.key]"
                type="textarea"
                :rows="3"
                :placeholder="item.placeholder ?? '请输入'"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </template>
      <div class="config-form__footer">
        <el-button type="primary" :loading="saveLoading" @click="handleSave">保存</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </el-form>

    <el-collapse v-if="logs && logs.length > 0" class="config-form__logs">
      <el-collapse-item title="变更日志" name="logs">
        <el-table :data="logs" size="small" border>
          <el-table-column prop="time" label="时间" width="180" />
          <el-table-column prop="operator" label="操作人" width="120" />
          <el-table-column prop="content" label="变更内容" show-overflow-tooltip />
        </el-table>
      </el-collapse-item>
    </el-collapse>
  </div>
</template>

<style scoped>
.config-form__label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.config-form__tip {
  color: var(--el-text-color-placeholder);
  cursor: help;
}

.config-form__number,
.config-form :deep(.el-select) {
  width: 100%;
}

.config-form__footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.config-form__logs {
  margin-top: 16px;
}
</style>
