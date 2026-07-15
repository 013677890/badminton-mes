<script setup lang="ts">
import { ref } from 'vue'
import { Aim, Promotion } from '@element-plus/icons-vue'
import { useScan } from '@/composables/useScan'

defineOptions({ name: 'TouchScanPage' })

const props = withDefaults(
  defineProps<{
    title?: string
    /** 扫码区提示文案 */
    tip?: string
    minLength?: number
    /** 同码冷却（ms），防扫码枪连击重复提交 */
    cooldownMs?: number
    /** 外部处理中：扫码区转 loading，暂不接受新码 */
    loading?: boolean
  }>(),
  {
    title: '扫码作业',
    tip: '使用扫码枪对准条码扫描，无需点击输入框',
    minLength: 4,
    cooldownMs: 800,
    loading: false,
  },
)

const emit = defineEmits<{
  scan: [code: string]
}>()

/** 全局键盘捕获：扫码枪即扫即得，页面无需聚焦输入框 */
const { scanning, lastCode, submit } = useScan({
  onScan: (code) => {
    if (props.loading) return
    emit('scan', code)
  },
  minLength: props.minLength,
  cooldownMs: props.cooldownMs,
  global: true,
})

const manualCode = ref('')

async function handleManualSubmit() {
  const code = manualCode.value.trim()
  if (!code) return
  await submit(code)
  manualCode.value = ''
}
</script>

<template>
  <div class="touch-scan-page">
    <div class="touch-scan-page__title">{{ title }}</div>

    <div
      v-loading="loading || scanning"
      element-loading-text="处理中..."
      class="touch-scan-page__zone"
    >
      <el-icon :size="72" color="var(--el-color-primary)"><Aim /></el-icon>
      <div class="touch-scan-page__tip">{{ tip }}</div>
      <div v-if="lastCode" class="touch-scan-page__last">
        最近扫码：<span class="touch-scan-page__code">{{ lastCode }}</span>
      </div>
    </div>

    <div class="touch-scan-page__manual">
      <el-input
        v-model="manualCode"
        size="large"
        :placeholder="`手工输入条码（不少于 ${minLength} 位）`"
        clearable
        @keyup.enter="handleManualSubmit"
      />
      <el-button
        type="primary"
        size="large"
        :icon="Promotion"
        :disabled="loading"
        @click="handleManualSubmit"
      >
        确认
      </el-button>
    </div>

    <!-- 扫码结果区：报工反馈、任务信息等 -->
    <div v-if="$slots.default" class="touch-scan-page__result">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.touch-scan-page__title {
  margin-bottom: 16px;
  font-size: 20px;
  font-weight: 600;
  text-align: center;
}

.touch-scan-page__zone {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: center;
  justify-content: center;
  min-height: 240px;
  background: var(--el-color-primary-light-9);
  border: 2px dashed var(--el-color-primary-light-5);
  border-radius: 12px;
}

.touch-scan-page__tip {
  font-size: 16px;
  color: var(--el-text-color-secondary);
}

.touch-scan-page__last {
  font-size: 15px;
  color: var(--el-text-color-secondary);
}

.touch-scan-page__code {
  font-size: 18px;
  font-weight: 600;
  color: var(--el-color-primary);
  font-variant-numeric: tabular-nums;
}

.touch-scan-page__manual {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.touch-scan-page__manual :deep(.el-input) {
  flex: 1;
}

.touch-scan-page__result {
  margin-top: 16px;
}
</style>
