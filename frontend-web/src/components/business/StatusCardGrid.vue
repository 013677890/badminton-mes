<script setup lang="ts">
import { computed } from 'vue'
import type { StatusCardItem, StatusMap } from '@/types/components'
import EmptyState from '@/components/base/EmptyState.vue'
import StatusTag from '@/components/base/StatusTag.vue'

defineOptions({ name: 'StatusCardGrid' })

const props = withDefaults(
  defineProps<{
    cards: StatusCardItem[]
    statusMap?: StatusMap
    /** 每行卡片数（按 24 栅格换算，需能整除） */
    columns?: number
    clickable?: boolean
  }>(),
  { columns: 4, clickable: true },
)

const emit = defineEmits<{
  'card-click': [card: StatusCardItem]
}>()

const span = computed(() => Math.floor(24 / props.columns))

/** 无状态或无映射时用边框灰，保持竖条占位对齐 */
function barColor(card: StatusCardItem): string {
  const type = card.status ? props.statusMap?.[card.status]?.type : undefined
  return type ? `var(--el-color-${type})` : 'var(--el-border-color)'
}

function handleClick(card: StatusCardItem) {
  if (props.clickable) emit('card-click', card)
}
</script>

<template>
  <div class="status-card-grid">
    <EmptyState v-if="cards.length === 0" />
    <el-row v-else :gutter="16">
      <!-- sm 媒体查询无上限会盖掉 span，md 起显式回到 columns 布局 -->
      <el-col v-for="card in cards" :key="card.key" :span="span" :xs="24" :sm="12" :md="span">
        <div
          class="status-card-grid__card"
          :class="{ 'status-card-grid__card--clickable': clickable }"
          @click="handleClick(card)"
        >
          <div class="status-card-grid__bar" :style="{ backgroundColor: barColor(card) }" />
          <div class="status-card-grid__body">
            <div class="status-card-grid__head">
              <div class="status-card-grid__titles">
                <div class="status-card-grid__title">{{ card.title }}</div>
                <div v-if="card.subtitle" class="status-card-grid__subtitle">
                  {{ card.subtitle }}
                </div>
              </div>
              <StatusTag v-if="card.status" :status="card.status" :status-map="statusMap" />
            </div>
            <div v-if="card.metrics?.length" class="status-card-grid__metrics">
              <div
                v-for="metric in card.metrics"
                :key="metric.label"
                class="status-card-grid__metric"
              >
                <div class="status-card-grid__metric-value">
                  {{ metric.value }}
                  <span v-if="metric.unit" class="status-card-grid__metric-unit">
                    {{ metric.unit }}
                  </span>
                </div>
                <div class="status-card-grid__metric-label">{{ metric.label }}</div>
              </div>
            </div>
            <slot name="card-extra" :card="card" />
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.status-card-grid__card {
  display: flex;
  margin-bottom: 16px;
  overflow: hidden;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  transition:
    box-shadow 0.2s,
    border-color 0.2s;
}

.status-card-grid__card--clickable {
  cursor: pointer;
}

.status-card-grid__card--clickable:hover {
  border-color: var(--el-color-primary-light-5);
  box-shadow: var(--el-box-shadow-light);
}

/* 左侧竖状态色条：比整卡染色更克制，多状态并排时不刺眼 */
.status-card-grid__bar {
  flex-shrink: 0;
  width: 4px;
}

.status-card-grid__body {
  flex: 1;
  min-width: 0;
  padding: 14px 16px;
}

.status-card-grid__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.status-card-grid__titles {
  min-width: 0;
}

.status-card-grid__title {
  overflow: hidden;
  font-size: 15px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-card-grid__subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.status-card-grid__metrics {
  display: flex;
  gap: 24px;
  margin-top: 12px;
}

.status-card-grid__metric-value {
  font-size: 20px;
  font-weight: 600;
  line-height: 1.2;
}

.status-card-grid__metric-unit {
  margin-left: 2px;
  font-size: 12px;
  font-weight: 400;
  color: var(--el-text-color-regular);
}

.status-card-grid__metric-label {
  margin-top: 2px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
