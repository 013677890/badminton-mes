<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ArrowLeft, ArrowRight, Document } from '@element-plus/icons-vue'
import type { SopMediaItem } from '@/types/components'
import EmptyState from '@/components/base/EmptyState.vue'

defineOptions({ name: 'TouchSopViewer' })

const props = withDefaults(
  defineProps<{
    /** SOP 步骤媒体列表，按步骤顺序 */
    items: SopMediaItem[]
    title?: string
    startIndex?: number
  }>(),
  { startIndex: 0 },
)

const emit = defineEmits<{
  change: [index: number]
}>()

const current = ref(props.startIndex)

watch(
  () => props.items,
  () => {
    current.value = 0
  },
)

const currentItem = computed(() => props.items[current.value])

function go(step: number) {
  const next = current.value + step
  if (next < 0 || next >= props.items.length) return
  current.value = next
  emit('change', next)
}

function openDoc(url: string) {
  window.open(url, '_blank')
}

/** 图片大图预览列表 */
const previewList = computed(() =>
  props.items.filter((item) => item.type === 'image').map((item) => item.url),
)

const previewIndex = computed(() => {
  const item = currentItem.value
  if (!item || item.type !== 'image') return 0
  return Math.max(previewList.value.indexOf(item.url), 0)
})
</script>

<template>
  <div class="touch-sop-viewer">
    <div class="touch-sop-viewer__header">
      <span class="touch-sop-viewer__title">{{ title ?? '作业指导书' }}</span>
      <span v-if="items.length" class="touch-sop-viewer__step">
        第 {{ current + 1 }} / {{ items.length }} 步
        <template v-if="currentItem?.title">：{{ currentItem.title }}</template>
      </span>
    </div>

    <div v-if="items.length" class="touch-sop-viewer__body">
      <el-button
        size="large"
        :icon="ArrowLeft"
        circle
        class="touch-sop-viewer__nav"
        :disabled="current === 0"
        @click="go(-1)"
      />
      <div class="touch-sop-viewer__stage">
        <el-image
          v-if="currentItem?.type === 'image'"
          :src="currentItem.url"
          fit="contain"
          :preview-src-list="previewList"
          :initial-index="previewIndex"
          class="touch-sop-viewer__media"
        />
        <video
          v-else-if="currentItem?.type === 'video'"
          :src="currentItem.url"
          controls
          class="touch-sop-viewer__media"
        />
        <div v-else-if="currentItem" class="touch-sop-viewer__doc">
          <el-icon :size="56" color="var(--el-color-primary)"><Document /></el-icon>
          <div class="touch-sop-viewer__doc-title">{{ currentItem.title ?? '工艺文件' }}</div>
          <el-button type="primary" size="large" @click="openDoc(currentItem.url)">
            打开文档
          </el-button>
        </div>
      </div>
      <el-button
        size="large"
        :icon="ArrowRight"
        circle
        class="touch-sop-viewer__nav"
        :disabled="current === items.length - 1"
        @click="go(1)"
      />
    </div>
    <EmptyState v-else description="暂无 SOP 内容" />

    <div v-if="items.length > 1" class="touch-sop-viewer__dots">
      <span
        v-for="(_, index) in items"
        :key="index"
        class="touch-sop-viewer__dot"
        :class="{ 'touch-sop-viewer__dot--active': index === current }"
        @click="current = index; emit('change', index)"
      />
    </div>
  </div>
</template>

<style scoped>
.touch-sop-viewer__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.touch-sop-viewer__title {
  font-size: 18px;
  font-weight: 600;
}

.touch-sop-viewer__step {
  font-size: 15px;
  color: var(--el-text-color-secondary);
}

.touch-sop-viewer__body {
  display: flex;
  gap: 12px;
  align-items: center;
}

.touch-sop-viewer__stage {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  min-height: 320px;
  overflow: hidden;
  background: var(--el-fill-color-light);
  border-radius: 8px;
}

.touch-sop-viewer__media {
  width: 100%;
  height: 320px;
}

.touch-sop-viewer__nav {
  flex-shrink: 0;
  width: 56px;
  height: 56px;
}

.touch-sop-viewer__doc {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: center;
}

.touch-sop-viewer__doc-title {
  font-size: 16px;
}

.touch-sop-viewer__dots {
  display: flex;
  gap: 10px;
  justify-content: center;
  margin-top: 12px;
}

.touch-sop-viewer__dot {
  width: 12px;
  height: 12px;
  cursor: pointer;
  background: var(--el-border-color);
  border-radius: 50%;
  transition: background 0.15s, transform 0.15s;
}

.touch-sop-viewer__dot--active {
  background: var(--el-color-primary);
  transform: scale(1.2);
}
</style>
