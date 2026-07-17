<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { ColumnDef, FilterField, OptionItem, RowAction } from '@/types/components'
import FilterTable from '@/components/business/FilterTable.vue'
import FormDialog from '@/components/base/FormDialog.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import PermissionButton from '@/components/base/PermissionButton.vue'
import { useTable } from '@/composables/useTable'
import { useFormDialog } from '@/composables/useFormDialog'
import { ENABLE_STATUS_MAP, ENABLE_STATUS_OPTIONS, ROLES } from '@/constants/production'
import { loadLineOptions, loadWorkshopOptions } from '@/api/production/options'
import {
  createUser,
  deleteUser,
  getEnabledRoles,
  getUserPage,
  resetUserPassword,
  updateUser,
  updateUserStatus,
} from '@/api/system'
import type { SystemUser, UserPageParams } from '@/api/system'

defineOptions({ name: 'UserList' })

/** UserController 整体仅 ADMIN 可用 */
const SYSTEM_MANAGE_ROLES = [ROLES.ADMIN]

// ---------- 下拉选项 ----------

const workshopOptions = ref<OptionItem[]>([])
const roleOptions = ref<OptionItem[]>([])
const lineOptions = ref<OptionItem[]>([])
const lineLoading = ref(false)

const filterFields = ref<FilterField[]>([
  { prop: 'userNo', label: '工号', type: 'input' },
  { prop: 'userName', label: '姓名', type: 'input' },
  { prop: 'workshopId', label: '车间', type: 'select', options: [] },
  { prop: 'roleId', label: '角色', type: 'select', options: [] },
  { prop: 'status', label: '状态', type: 'select', options: ENABLE_STATUS_OPTIONS },
])

onMounted(async () => {
  try {
    const [workshops, roles] = await Promise.all([loadWorkshopOptions(), getEnabledRoles()])
    workshopOptions.value = workshops
    roleOptions.value = roles.map((role) => ({
      label: `${role.roleName}（${role.roleCode}）`,
      value: role.id,
    }))
    const workshopField = filterFields.value.find((item) => item.prop === 'workshopId')
    if (workshopField) workshopField.options = workshops
    const roleField = filterFields.value.find((item) => item.prop === 'roleId')
    if (roleField) roleField.options = roleOptions.value
  } catch {
    // 下拉加载失败不阻塞列表
  }
})

function workshopLabel(workshopId: number | null): string {
  if (workshopId === null) return '-'
  return String(workshopOptions.value.find((opt) => opt.value === workshopId)?.label ?? workshopId)
}

// ---------- 列表 ----------

const columns: ColumnDef<SystemUser>[] = [
  { prop: 'userNo', label: '工号', width: 110 },
  { prop: 'userName', label: '姓名', width: 110 },
  { prop: 'mobile', label: '手机号', width: 130, formatter: (row) => row.mobile ?? '-' },
  {
    prop: 'workshopId',
    label: '所属车间',
    minWidth: 130,
    formatter: (row) => workshopLabel(row.workshopId),
  },
  {
    prop: 'roleNames',
    label: '角色',
    minWidth: 170,
    showOverflowTooltip: true,
    formatter: (row) => (row.roleNames.length ? row.roleNames.join('、') : '-'),
  },
  { prop: 'status', label: '状态', width: 80, statusMap: ENABLE_STATUS_MAP },
  { prop: 'createTime', label: '创建时间', width: 170 },
]

const rowActions: RowAction<SystemUser>[] = [
  { key: 'edit', label: '编辑', roles: SYSTEM_MANAGE_ROLES },
  {
    key: 'enable',
    label: '启用',
    type: 'success',
    roles: SYSTEM_MANAGE_ROLES,
    confirm: '确认启用该账号？',
    show: (row) => row.status === 0,
  },
  {
    key: 'disable',
    label: '停用',
    type: 'warning',
    roles: SYSTEM_MANAGE_ROLES,
    confirm: '停用后该账号立即强制下线且无法登录，确认？',
    show: (row) => row.status === 1,
  },
  { key: 'resetPwd', label: '重置密码', roles: SYSTEM_MANAGE_ROLES },
  {
    key: 'delete',
    label: '删除',
    type: 'danger',
    roles: SYSTEM_MANAGE_ROLES,
    confirm: '逻辑删除该账号（不能删除当前登录账号），确认？',
  },
]

const { data, loading, pagination, query, reset, refresh, onPageChange } = useTable<
  SystemUser,
  UserPageParams
>({ fetcher: getUserPage })

// ---------- 新增 / 编辑 ----------

interface UserForm {
  id?: number
  userNo: string
  userName: string
  password: string
  mobile: string
  workshopId: number | null
  lineId: number | null
  roleIds: number[]
}

const dialog = useFormDialog<UserForm>(
  () => ({
    userNo: '',
    userName: '',
    password: '',
    mobile: '',
    workshopId: null,
    lineId: null,
    roleIds: [],
  }),
  {
    titles: { create: '新增用户', edit: '编辑用户' },
    submit: async (model, mode) => {
      const payload = {
        userNo: model.userNo,
        userName: model.userName,
        mobile: model.mobile || undefined,
        workshopId: model.workshopId ?? undefined,
        lineId: model.lineId ?? undefined,
        roleIds: model.roleIds,
      }
      if (mode === 'create') {
        await createUser({ ...payload, password: model.password })
        ElMessage.success('用户已创建')
      } else {
        await updateUser(model.id!, payload)
        ElMessage.success('用户已更新')
      }
    },
    onSuccess: refresh,
  },
)

const rules = {
  userNo: [
    { required: true, message: '请输入工号', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]+$/, message: '仅限字母、数字、下划线和连字符', trigger: 'blur' },
  ],
  userName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度 6-32 位', trigger: 'blur' },
  ],
  mobile: [
    { pattern: /^1\d{10}$/, message: '请输入 11 位手机号', trigger: 'blur' },
  ],
  roleIds: [{ required: true, message: '请至少选择一个角色', trigger: 'change' }],
}

/** 车间联动产线：切车间时重载产线选项并清空已选产线 */
watch(
  () => dialog.model.value.workshopId,
  async (workshopId, previous) => {
    if (previous !== undefined && workshopId !== previous) {
      dialog.model.value.lineId = null
    }
    lineOptions.value = []
    if (!workshopId) return
    lineLoading.value = true
    try {
      lineOptions.value = await loadLineOptions(workshopId)
    } catch {
      // 下拉加载失败不阻塞表单
    } finally {
      lineLoading.value = false
    }
  },
)

// ---------- 重置密码 ----------

interface ResetPwdForm {
  id?: number
  userText: string
  newPassword: string
}

const resetPwdDialog = useFormDialog<ResetPwdForm>(
  () => ({ userText: '', newPassword: '' }),
  {
    titles: { edit: '重置密码' },
    submit: async (model) => {
      await resetUserPassword(model.id!, model.newPassword)
      ElMessage.success('密码已重置，该用户已强制下线')
    },
  },
)

const resetPwdRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度 6-32 位', trigger: 'blur' },
  ],
}

/** 编辑时原手机号的脱敏值（后端更新会整字段覆盖，展示在占位符提醒管理员） */
const editingMobileMasked = ref('未设置')

// ---------- 行操作 ----------

async function handleRowAction(key: string, row: SystemUser) {
  try {
    if (key === 'edit') {
      editingMobileMasked.value = row.mobile ?? '未设置'
      dialog.open('edit', {
        id: row.id,
        userNo: row.userNo,
        userName: row.userName,
        password: '',
        mobile: '',
        workshopId: row.workshopId,
        lineId: row.lineId,
        roleIds: [...row.roleIds],
      })
    } else if (key === 'enable' || key === 'disable') {
      await updateUserStatus(row.id, key === 'enable' ? 1 : 0)
      ElMessage.success(key === 'enable' ? '已启用' : '已停用并强制下线')
      await refresh()
    } else if (key === 'resetPwd') {
      resetPwdDialog.open('edit', {
        id: row.id,
        userText: `${row.userNo} ${row.userName}`,
        newPassword: '',
      })
    } else if (key === 'delete') {
      await deleteUser(row.id)
      ElMessage.success('已删除')
      await refresh()
    }
  } catch {
    // 失败提示由拦截器统一弹出
    await refresh()
  }
}
</script>

<template>
  <div class="page">
    <PageHeader
      title="用户管理"
      description="账号、角色与组织归属维护（仅系统管理员）：手机号脱敏展示，停用/重置密码会强制下线"
    />
    <FilterTable
      :filter-fields="filterFields"
      :columns="columns"
      :data="data"
      :loading="loading"
      :pagination="pagination"
      :row-actions="rowActions"
      :action-width="280"
      show-index
      @query="query"
      @reset="reset"
      @page-change="onPageChange"
      @row-action="handleRowAction"
    >
      <template #toolbar>
        <PermissionButton :roles="SYSTEM_MANAGE_ROLES" type="primary" @click="dialog.open()">
          新增用户
        </PermissionButton>
      </template>
    </FilterTable>

    <!-- 新增/编辑 -->
    <FormDialog
      v-model:visible="dialog.visible.value"
      :title="dialog.title.value"
      :model="dialog.model.value"
      :rules="rules"
      :submit-loading="dialog.submitLoading.value"
      @submit="dialog.handleSubmit"
    >
      <el-form-item label="工号" prop="userNo">
        <el-input
          v-model="dialog.model.value.userNo"
          :disabled="dialog.mode.value === 'edit'"
          maxlength="32"
          placeholder="如 W1001"
        />
      </el-form-item>
      <el-form-item label="姓名" prop="userName">
        <el-input v-model="dialog.model.value.userName" maxlength="64" />
      </el-form-item>
      <el-form-item v-if="dialog.mode.value === 'create'" label="初始密码" prop="password">
        <el-input
          v-model="dialog.model.value.password"
          type="password"
          show-password
          maxlength="32"
          placeholder="6-32 位，首次登录后建议修改"
        />
      </el-form-item>
      <el-form-item label="手机号" prop="mobile">
        <el-input
          v-model="dialog.model.value.mobile"
          maxlength="11"
          :placeholder="
            dialog.mode.value === 'edit'
              ? `原 ${editingMobileMasked}；留空提交将清空手机号`
              : '选填'
          "
        />
      </el-form-item>
      <el-form-item label="所属车间" prop="workshopId">
        <el-select
          v-model="dialog.model.value.workshopId"
          filterable
          clearable
          placeholder="选填，车间主管/班组长建议绑定"
        >
          <el-option
            v-for="opt in workshopOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="所属产线" prop="lineId">
        <el-select
          v-model="dialog.model.value.lineId"
          :loading="lineLoading"
          :disabled="!dialog.model.value.workshopId"
          filterable
          clearable
          placeholder="先选车间"
        >
          <el-option
            v-for="opt in lineOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="角色" prop="roleIds">
        <el-select v-model="dialog.model.value.roleIds" multiple placeholder="可多选">
          <el-option
            v-for="opt in roleOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
    </FormDialog>

    <!-- 重置密码 -->
    <FormDialog
      v-model:visible="resetPwdDialog.visible.value"
      :title="resetPwdDialog.title.value"
      :model="resetPwdDialog.model.value"
      :rules="resetPwdRules"
      :submit-loading="resetPwdDialog.submitLoading.value"
      width="480px"
      @submit="resetPwdDialog.handleSubmit"
    >
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="重置后该用户当前会话立即失效，需用新密码重新登录"
        class="dialog-tip"
      />
      <el-form-item label="用户">
        <span>{{ resetPwdDialog.model.value.userText }}</span>
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input
          v-model="resetPwdDialog.model.value.newPassword"
          type="password"
          show-password
          maxlength="32"
          placeholder="6-32 位"
        />
      </el-form-item>
    </FormDialog>
  </div>
</template>

<style scoped>
.page > :deep(.page-header) {
  margin-bottom: 16px;
}

.dialog-tip {
  margin-bottom: 16px;
}
</style>
