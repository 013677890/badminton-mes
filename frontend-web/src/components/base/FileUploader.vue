<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadProps, UploadUserFile } from 'element-plus'
import { Plus, Upload } from '@element-plus/icons-vue'

defineOptions({ name: 'FileUploader', inheritAttrs: false })

const props = withDefaults(
  defineProps<{
    /** 允许的文件类型，如 '.jpg,.png' 或 'image/*' */
    accept?: string
    /** 单文件大小上限（MB） */
    maxSizeMb?: number
    /** 数量上限 */
    limit?: number
    listType?: 'text' | 'picture' | 'picture-card'
    /** 上传地址；不传则仅收集文件由页面提交时统一上传 */
    action?: string
    tip?: string
  }>(),
  { maxSizeMb: 10, limit: 5, listType: 'text', action: '#' },
)

const fileList = defineModel<UploadUserFile[]>('fileList', { default: () => [] })

const emit = defineEmits<{
  change: [files: UploadUserFile[]]
  remove: [files: UploadUserFile[]]
}>()

/** 未显式传 action 时不自动上传，页面拿到 raw 文件后统一提交 */
const autoUpload = computed(() => props.action !== '#')

const tipText = computed(
  () =>
    props.tip ??
    `最多 ${props.limit} 个文件，单个不超过 ${props.maxSizeMb}MB${props.accept ? `，支持 ${props.accept}` : ''}`,
)

function validateFile(file: { name: string; size?: number }): boolean {
  if (file.size !== undefined && file.size > props.maxSizeMb * 1024 * 1024) {
    ElMessage.error(`文件「${file.name}」超过 ${props.maxSizeMb}MB 上限`)
    return false
  }
  if (props.accept) {
    const extensions = props.accept
      .split(',')
      .map((item) => item.trim().toLowerCase())
      .filter((item) => item.startsWith('.'))
    if (extensions.length > 0) {
      const name = file.name.toLowerCase()
      if (!extensions.some((ext) => name.endsWith(ext))) {
        ElMessage.error(`文件「${file.name}」类型不支持`)
        return false
      }
    }
  }
  return true
}

const handleBeforeUpload: UploadProps['beforeUpload'] = (rawFile) => validateFile(rawFile)

/** autoUpload=false 时 beforeUpload 不触发，在 change 里校验并剔除非法文件 */
const handleChange: UploadProps['onChange'] = (file, files) => {
  if (file.status === 'ready' && !autoUpload.value && !validateFile(file)) {
    const index = files.indexOf(file)
    if (index !== -1) files.splice(index, 1)
    return
  }
  emit('change', files)
}

const handleRemove: UploadProps['onRemove'] = (_file, files) => {
  emit('remove', files)
}

const handleExceed: UploadProps['onExceed'] = () => {
  ElMessage.warning(`最多只能上传 ${props.limit} 个文件`)
}
</script>

<template>
  <el-upload
    v-model:file-list="fileList"
    :accept="accept"
    :limit="limit"
    :list-type="listType"
    :action="action"
    :auto-upload="autoUpload"
    :before-upload="handleBeforeUpload"
    :on-change="handleChange"
    :on-remove="handleRemove"
    :on-exceed="handleExceed"
    v-bind="$attrs"
  >
    <el-icon v-if="listType === 'picture-card'"><Plus /></el-icon>
    <el-button v-else type="primary" :icon="Upload">选择文件</el-button>
    <template #tip>
      <div class="el-upload__tip">{{ tipText }}</div>
    </template>
  </el-upload>
</template>
