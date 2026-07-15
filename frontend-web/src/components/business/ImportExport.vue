<script setup lang="ts">
import { computed, ref } from 'vue'
import type { UploadUserFile } from 'element-plus'
import { Download, Upload } from '@element-plus/icons-vue'
import type { ImportResult } from '@/types/components'
import FileUploader from '@/components/base/FileUploader.vue'
import PermissionButton from '@/components/base/PermissionButton.vue'

defineOptions({ name: 'ImportExport' })

const props = withDefaults(
  defineProps<{
    /** 导入模板下载地址，不传则不展示模板链接 */
    templateUrl?: string
    importing?: boolean
    exporting?: boolean
    /** 后端导入回执；重新导入前由父层重置为 null */
    importResult?: ImportResult | null
    accept?: string
    roles?: string[]
  }>(),
  {
    importing: false,
    exporting: false,
    importResult: null,
    accept: '.xlsx,.xls,.csv',
  },
)

const emit = defineEmits<{
  import: [file: File]
  export: []
  'template-download': []
}>()

const dialogVisible = ref(false)
const fileList = ref<UploadUserFile[]>([])

const selectedFile = computed(() => fileList.value[0]?.raw as File | undefined)

/** 打开时清空上次残留文件；importResult 的重置由父层负责 */
function openImport() {
  fileList.value = []
  dialogVisible.value = true
}

function closeImport() {
  dialogVisible.value = false
}

function handleTemplateDownload() {
  emit('template-download')
  if (props.templateUrl) window.open(props.templateUrl)
}

function handleImport() {
  if (!selectedFile.value) return
  emit('import', selectedFile.value)
}

defineExpose({ openImport, closeImport })
</script>

<template>
  <div class="import-export">
    <PermissionButton :roles="roles" :icon="Upload" @click="openImport">导入</PermissionButton>
    <PermissionButton
      :roles="roles"
      type="primary"
      :icon="Download"
      :loading="exporting"
      @click="emit('export')"
    >
      导出
    </PermissionButton>

    <el-dialog v-model="dialogVisible" title="数据导入" width="520px">
      <FileUploader v-model:file-list="fileList" :accept="accept" :limit="1" />
      <div v-if="templateUrl" class="import-export__template">
        <el-button link type="primary" @click="handleTemplateDownload">下载导入模板</el-button>
      </div>
      <template v-if="importResult">
        <el-alert
          :type="importResult.failCount > 0 ? 'warning' : 'success'"
          :title="`导入完成：成功 ${importResult.successCount} 条，失败 ${importResult.failCount} 条`"
          :closable="false"
          show-icon
          class="import-export__result"
        />
        <el-table
          v-if="importResult.errors.length > 0"
          :data="importResult.errors"
          size="small"
          border
          max-height="240"
          class="import-export__errors"
        >
          <el-table-column prop="row" label="行号" width="80" align="center" />
          <el-table-column prop="message" label="错误信息" />
        </el-table>
      </template>
      <template #footer>
        <el-button @click="closeImport">取 消</el-button>
        <el-button
          type="primary"
          :loading="importing"
          :disabled="!selectedFile"
          @click="handleImport"
        >
          开始导入
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.import-export {
  display: inline-flex;
  gap: 8px;
  align-items: center;
}

/* 间距交给 gap，去掉 Element 相邻按钮默认 margin 避免叠加 */
.import-export > :deep(.el-button + .el-button) {
  margin-left: 0;
}

.import-export__template {
  margin-top: 8px;
}

.import-export__result {
  margin-top: 12px;
}

.import-export__errors {
  margin-top: 12px;
}
</style>
