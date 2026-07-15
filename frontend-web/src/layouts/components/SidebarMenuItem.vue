<script setup lang="ts">
import { computed } from 'vue'
import type { RouteRecordRaw } from 'vue-router'
import { usePermission } from '@/composables/usePermission'

defineOptions({ name: 'SidebarMenuItem' })

const props = defineProps<{ route: RouteRecordRaw }>()

const { hasRole } = usePermission()

const visibleChildren = computed(() =>
  (props.route.children ?? []).filter(
    (child) => !child.meta?.hidden && hasRole(child.meta?.roles),
  ),
)

const title = computed(() => props.route.meta?.title ?? String(props.route.name ?? ''))
const icon = computed(() => props.route.meta?.icon)
</script>

<template>
  <el-sub-menu v-if="visibleChildren.length > 0" :index="route.path">
    <template #title>
      <el-icon v-if="icon"><component :is="icon" /></el-icon>
      <span>{{ title }}</span>
    </template>
    <!-- 递归渲染多级菜单 -->
    <SidebarMenuItem v-for="child in visibleChildren" :key="child.path" :route="child" />
  </el-sub-menu>
  <el-menu-item v-else :index="route.path">
    <el-icon v-if="icon"><component :is="icon" /></el-icon>
    <template #title>{{ title }}</template>
  </el-menu-item>
</template>
