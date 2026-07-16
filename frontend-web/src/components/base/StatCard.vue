<script setup lang="ts">
import { computed } from 'vue'
import { CaretBottom, CaretTop } from '@element-plus/icons-vue'
import { formatNumber } from '@/utils/format'

defineOptions({ name: 'StatCard' })

const props = withDefaults(
  defineProps<{
    label: string
    value: number | string
    unit?: string
    /** 全局注册的 Element 图标名，如 'TrendCharts' */
    icon?: string
    /** 图标底色 */
    iconColor?: string
    /** 环比趋势百分比，正数向上、负数向下 */
    trend?: number
    trendLabel?: string
  }>(),
  { iconColor: 'var(--el-color-primary)', trendLabel: '较昨日' },
)

const displayValue = computed(() =>
  typeof props.value === 'number' ? formatNumber(props.value) : props.value,
)

const trendClass = computed(() => {
  if (props.trend === undefined || props.trend === 0) return 'stat-card__trend--flat'
  return props.trend > 0 ? 'stat-card__trend--up' : 'stat-card__trend--down'
})
</script>

<template>
  <div class="stat-card">
    <div v-if="icon" class="stat-card__icon" :style="{ backgroundColor: iconColor }">
      <el-icon :size="24" color="#fff"><component :is="icon" /></el-icon>
    </div>
    <div class="stat-card__body">
      <div class="stat-card__label">{{ label }}</div>
      <div class="stat-card__value">
        {{ displayValue }}
        <span v-if="unit" class="stat-card__unit">{{ unit }}</span>
      </div>
      <div v-if="trend !== undefined" class="stat-card__trend" :class="trendClass">
        {{ trendLabel }}
        <el-icon v-if="trend > 0"><CaretTop /></el-icon>
        <el-icon v-else-if="trend < 0"><CaretBottom /></el-icon>
        {{ Math.abs(trend) }}%
      </div>
    </div>
    <slot name="extra" />
  </div>
</template>

<style scoped>
.stat-card {
  display: flex;
  gap: 16px;
  align-items: center;
  padding: 20px;
  background: var(--el-bg-color);
  border-radius: 4px;
  box-shadow: var(--el-box-shadow-light);
}

.stat-card__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 8px;
  flex-shrink: 0;
}

.stat-card__label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.stat-card__value {
  margin-top: 4px;
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
}

.stat-card__unit {
  font-size: 13px;
  font-weight: 400;
  color: var(--el-text-color-secondary);
}

.stat-card__trend {
  display: flex;
  align-items: center;
  gap: 2px;
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.stat-card__trend--up {
  color: var(--el-color-success);
}

.stat-card__trend--down {
  color: var(--el-color-danger);
}
</style>
