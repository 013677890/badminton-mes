<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import PermissionButton from '@/components/base/PermissionButton.vue'

defineOptions({ name: 'ApprovalActionBar' })

const props = withDefaults(
  defineProps<{
    /** 可审批角色，无权限时隐藏按钮；后端 @RequiresRoles 是最终防线 */
    roles?: string[]
    rejectReasonRequired?: boolean
    /** 通过时是否弹出可选意见框 */
    approveCommentEnabled?: boolean
    loading?: boolean
    /** 吸附页面底部（长表单审批场景） */
    sticky?: boolean
  }>(),
  {
    rejectReasonRequired: true,
    approveCommentEnabled: false,
    loading: false,
    sticky: false,
  },
)

const emit = defineEmits<{
  approve: [comment?: string]
  reject: [reason: string]
}>()

const dialogVisible = ref(false)
const mode = ref<'approve' | 'reject'>('reject')
const formRef = ref<FormInstance>()
const form = reactive({ comment: '' })

const isReject = computed(() => mode.value === 'reject')
const dialogTitle = computed(() => (isReject.value ? '驳回审核' : '通过审核'))

/** required 校验会放过纯空格，用自定义 validator 兜底 */
const rules = computed<FormRules>(() => ({
  comment:
    isReject.value && props.rejectReasonRequired
      ? [
          {
            required: true,
            validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
              if (!value || !value.trim()) callback(new Error('请填写审核意见'))
              else callback()
            },
            trigger: 'blur',
          },
        ]
      : [],
}))

function openDialog(next: 'approve' | 'reject') {
  mode.value = next
  form.comment = ''
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

function handleApprove() {
  if (props.approveCommentEnabled) {
    openDialog('approve')
    return
  }
  emit('approve')
}

async function handleConfirm() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  const comment = form.comment.trim()
  if (isReject.value) {
    emit('reject', comment)
  } else {
    emit('approve', comment || undefined)
  }
  dialogVisible.value = false
}
</script>

<template>
  <div class="approval-action-bar" :class="{ 'approval-action-bar--sticky': sticky }">
    <div class="approval-action-bar__extra">
      <slot name="extra" />
    </div>
    <div class="approval-action-bar__actions">
      <PermissionButton
        :roles="roles"
        mode="hide"
        type="danger"
        plain
        :loading="loading"
        @click="openDialog('reject')"
      >
        驳 回
      </PermissionButton>
      <PermissionButton
        :roles="roles"
        mode="hide"
        type="primary"
        :loading="loading"
        @click="handleApprove"
      >
        通 过
      </PermissionButton>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item prop="comment" label="审核意见">
          <el-input
            v-model="form.comment"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            :placeholder="
              isReject && rejectReasonRequired ? '请填写驳回原因' : '请填写审核意见（选填）'
            "
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button
          :type="isReject ? 'danger' : 'primary'"
          :loading="loading"
          @click="handleConfirm"
        >
          确 定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.approval-action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

/* 吸附底部：长详情页滚动时审批按钮常驻可见 */
.approval-action-bar--sticky {
  position: sticky;
  bottom: 0;
  z-index: 10;
  padding: 12px 16px;
  background: var(--el-bg-color);
  border-top: 1px solid var(--el-border-color-lighter);
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.04);
}

.approval-action-bar__extra {
  display: flex;
  gap: 8px;
  align-items: center;
}

.approval-action-bar__actions {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-left: auto;
}
</style>
