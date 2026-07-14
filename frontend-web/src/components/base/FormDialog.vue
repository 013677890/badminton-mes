<script setup lang="ts">
import { ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'

defineOptions({ name: 'FormDialog', inheritAttrs: false })

withDefaults(
  defineProps<{
    title: string
    /** 表单数据对象（el-form :model） */
    model: Record<string, any>
    rules?: FormRules
    width?: string | number
    labelWidth?: string
    submitLoading?: boolean
    /** 只读查看模式：禁用表单、隐藏确定按钮 */
    readonly?: boolean
    /** 内容区最大高度，超出滚动 */
    maxHeight?: string
  }>(),
  {
    width: '640px',
    labelWidth: '100px',
    submitLoading: false,
    readonly: false,
    maxHeight: '62vh',
  },
)

const visible = defineModel<boolean>('visible', { default: false })

const emit = defineEmits<{
  /** 校验通过后触发，提交逻辑由页面处理 */
  submit: []
  cancel: []
}>()

const formRef = ref<FormInstance>()

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  emit('submit')
}

function handleCancel() {
  visible.value = false
  emit('cancel')
}

defineExpose({
  formRef,
  validate: () => formRef.value?.validate(),
  resetFields: () => formRef.value?.resetFields(),
  clearValidate: () => formRef.value?.clearValidate(),
})
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="title"
    :width="width"
    destroy-on-close
    :close-on-click-modal="false"
    append-to-body
    v-bind="$attrs"
  >
    <el-scrollbar :max-height="maxHeight">
      <el-form
        ref="formRef"
        :model="model"
        :rules="rules"
        :label-width="labelWidth"
        :disabled="readonly"
        class="form-dialog__form"
      >
        <slot />
      </el-form>
    </el-scrollbar>
    <template #footer>
      <slot name="footer">
        <el-button @click="handleCancel">取消</el-button>
        <el-button
          v-if="!readonly"
          type="primary"
          :loading="submitLoading"
          @click="handleSubmit"
        >
          确定
        </el-button>
      </slot>
    </template>
  </el-dialog>
</template>

<style scoped>
.form-dialog__form {
  padding-right: 12px;
}
</style>
