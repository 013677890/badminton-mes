<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Aim } from '@element-plus/icons-vue'
import type { InputInstance } from 'element-plus'

defineOptions({ name: 'ScanInput' })

const props = withDefaults(
  defineProps<{
    placeholder?: string
    autofocus?: boolean
    /** 上游处理中：禁用输入避免连扫 */
    loading?: boolean
    minLength?: number
    /** 同一条码冷却时间（ms），防扫码枪连击重复提交 */
    cooldownMs?: number
    clearAfterScan?: boolean
    /** 自定义校验：true 通过；返回字符串作为错误提示 */
    validate?: (code: string) => boolean | string
    size?: 'default' | 'large'
  }>(),
  {
    placeholder: '请扫描条码，或输入后回车',
    autofocus: true,
    loading: false,
    minLength: 4,
    cooldownMs: 800,
    clearAfterScan: true,
    size: 'large',
  },
)

const emit = defineEmits<{
  scan: [code: string]
}>()

const code = ref('')
const error = ref('')
const inputRef = ref<InputInstance>()

let lastCode = ''
let lastAt = 0

function handleSubmit() {
  const value = code.value.trim()
  error.value = ''
  if (!value) return
  if (value.length < props.minLength) {
    error.value = `条码长度不能少于 ${props.minLength} 位`
    return
  }
  const now = Date.now()
  if (value === lastCode && now - lastAt < props.cooldownMs) return
  if (props.validate) {
    const result = props.validate(value)
    if (result !== true) {
      error.value = typeof result === 'string' ? result : '条码校验未通过'
      return
    }
  }
  lastCode = value
  lastAt = now
  emit('scan', value)
  if (props.clearAfterScan) code.value = ''
  inputRef.value?.focus()
}

onMounted(() => {
  if (props.autofocus) inputRef.value?.focus()
})

defineExpose({ focus: () => inputRef.value?.focus() })
</script>

<template>
  <div class="scan-input">
    <el-input
      ref="inputRef"
      v-model="code"
      :size="size"
      :placeholder="placeholder"
      :disabled="loading"
      clearable
      @keyup.enter="handleSubmit"
    >
      <template #prefix>
        <el-icon><Aim /></el-icon>
      </template>
      <template #append>
        <el-button :loading="loading" @click="handleSubmit">确认</el-button>
      </template>
    </el-input>
    <div v-if="error" class="scan-input__error">{{ error }}</div>
  </div>
</template>

<style scoped>
.scan-input__error {
  margin-top: 6px;
  font-size: 13px;
  color: var(--el-color-danger);
}
</style>
