<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { OptionItem } from '@/types/components'
import EmptyState from '@/components/base/EmptyState.vue'
import PageHeader from '@/components/base/PageHeader.vue'
import StatusTag from '@/components/base/StatusTag.vue'
import { ENABLE_STATUS_MAP } from '@/constants/production'
import { loadWorkshopOptions } from '@/api/production/options'
import { getEnabledRoles, getRoleUsers } from '@/api/system'
import type { Role, RoleUser } from '@/api/system'

defineOptions({ name: 'RoleList' })

/**
 * 角色为 V3 迁移内置的固定字典（无增删改接口），页面只读展示，
 * 支持下钻查看各角色成员；用户的角色分配在「用户管理」维护。
 */

const roles = ref<Role[]>([])
const loading = ref(false)
const workshopOptions = ref<OptionItem[]>([])

onMounted(async () => {
  loading.value = true
  try {
    const [roleList, workshops] = await Promise.all([
      getEnabledRoles(),
      loadWorkshopOptions().catch(() => [] as OptionItem[]),
    ])
    roles.value = roleList
    workshopOptions.value = workshops
  } finally {
    loading.value = false
  }
})

function workshopLabel(workshopId: number | null): string {
  if (workshopId === null) return '-'
  return String(workshopOptions.value.find((opt) => opt.value === workshopId)?.label ?? workshopId)
}

// ---------- 成员下钻 ----------

const drawerVisible = ref(false)
const currentRole = ref<Role>()
const users = ref<RoleUser[]>([])
const userLoading = ref(false)

async function openUsers(role: Role) {
  currentRole.value = role
  users.value = []
  drawerVisible.value = true
  userLoading.value = true
  try {
    users.value = await getRoleUsers(role.id)
  } finally {
    userLoading.value = false
  }
}
</script>

<template>
  <div class="page">
    <PageHeader
      title="角色管理"
      description="系统内置角色字典（只读）：权限按角色编码在后端接口上硬校验，用户的角色分配在用户管理中维护"
    />
    <el-card shadow="never">
      <el-table v-loading="loading" :data="roles" border>
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="roleCode" label="角色编码" width="180" />
        <el-table-column prop="roleName" label="角色名称" width="150" />
        <el-table-column prop="remark" label="说明" min-width="240">
          <template #default="{ row }">{{ row.remark ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <StatusTag :status="row.status" :status-map="ENABLE_STATUS_MAP" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openUsers(row as Role)">查看成员</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer
      v-model="drawerVisible"
      :title="`角色成员：${currentRole?.roleName ?? ''}（${currentRole?.roleCode ?? ''}）`"
      size="560px"
      destroy-on-close
    >
      <el-table
        v-if="userLoading || users.length"
        v-loading="userLoading"
        :data="users"
        border
        size="small"
      >
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="userNo" label="工号" width="120" />
        <el-table-column prop="userName" label="姓名" width="120" />
        <el-table-column label="所属车间" min-width="140">
          <template #default="{ row }">{{ workshopLabel(row.workshopId) }}</template>
        </el-table-column>
        <el-table-column label="产线 ID" width="90" align="right">
          <template #default="{ row }">{{ row.lineId ?? '-' }}</template>
        </el-table-column>
      </el-table>
      <EmptyState v-else description="该角色下暂无启用用户" />
    </el-drawer>
  </div>
</template>

<style scoped>
.page > :deep(.page-header) {
  margin-bottom: 16px;
}
</style>
