<script setup lang="ts">
import { computed, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { TouchFormField } from '@/types/components'

defineOptions({ name: 'TouchSimpleForm' })

const props = withDefaults(
  defineProps<{
    fields: TouchFormField[]
    submitText?: string
    submitLoading?: boolean
    /** 隐藏内置提交按钮（外部用 TouchActionButtons 时） */
    hideSubmit?: boolean
  }>(),
  { submitText: '提交', submitLoading: false, hideSubmit: false },
)

/** 表单数据由父组件持有，组件只负责渲染大号控件 */
const model = defineModel<Record<string, any>>({ required: true })

const emit = defineEmits<{
  submit: [model: Record<string, any>]
}>()

const formRef = ref<FormInstance>()

const rules = computed<FormRules>(() => {
  const result: FormRules = {}
  for (const field of props.fields) {
    if (field.required) {
      result[field.prop] = [
        {
          required: true,
          message: `请${field.type === 'select' ? '选择' : '输入'}${field.label}`,
          trigger: field.type === 'select' ? 'change' : 'blur',
        },
      ]
    }
  }
  return result
})

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  emit('submit', { ...model.value })
}

defineExpose({
  validate: () => formRef.value?.validate().catch(() => false),
  resetFields: () => formRef.value?.resetFields(),
})
</script>

<template>
  <el-form
    ref="formRef"
    class="touch-simple-form"
    :model="model"
    :rules="rules"
    label-position="top"
    size="large"
    @submit.prevent="handleSubmit"
  >
    <el-form-item
      v-for="field in fields"
      :key="field.prop"
      :label="field.label"
      :prop="field.prop"
    >
      <el-input
        v-if="field.type === 'text'"
        v-model="model[field.prop]"
        :placeholder="field.placeholder ?? `请输入${field.label}`"
        clearable
      />
      <el-input-number
        v-else-if="field.type === 'number'"
        v-model="model[field.prop]"
        :min="0"
        class="touch-simple-form__number"
      />
      <el-select
        v-else-if="field.type === 'select'"
        v-model="model[field.prop]"
        :placeholder="field.placeholder ?? `请选择${field.label}`"
        clearable
      >
        <el-option
          v-for="opt in field.options ?? []"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
          :disabled="opt.disabled"
        />
      </el-select>
    </el-form-item>
    <el-form-item v-if="!hideSubmit">
      <el-button
        type="primary"
        size="large"
        class="touch-simple-form__submit"
        :loading="submitLoading"
        @click="handleSubmit"
      >
        {{ submitText }}
      </el-button>
    </el-form-item>
  </el-form>
</template>

<style scoped>
.touch-simple-form :deep(.el-form-item__label) {
  font-size: 16px;
  font-weight: 500;
}

.touch-simple-form__number {
  width: 100%;
}

/* 大号加减按钮，方便戴手套操作 */
.touch-simple-form__number :deep(.el-input-number__decrease),
.touch-simple-form__number :deep(.el-input-number__increase) {
  width: 48px;
}

.touch-simple-form__submit {
  width: 100%;
  height: 56px;
  font-size: 18px;
  font-weight: 600;
}
</style>
