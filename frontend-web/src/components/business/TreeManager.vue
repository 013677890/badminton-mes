<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import type { TreeInstance } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import type { TreeNodeData, TreeOperation } from '@/types/components'

defineOptions({ name: 'TreeManager' })

withDefaults(
  defineProps<{
    data: TreeNodeData[]
    title?: string
    /** 允许的节点操作，控制行内按钮与右键菜单 */
    operations?: TreeOperation[]
    defaultExpandAll?: boolean
  }>(),
  {
    operations: () => ['add', 'edit', 'disable', 'delete'],
    defaultExpandAll: true,
  },
)

const emit = defineEmits<{
  'node-click': [node: TreeNodeData]
  'node-operate': [op: TreeOperation, node: TreeNodeData | null]
}>()

const treeRef = ref<TreeInstance>()
const filterText = ref('')

watch(filterText, (value) => treeRef.value?.filter(value))

function filterNode(value: string, nodeData: Record<string, any>): boolean {
  if (!value) return true
  return String(nodeData.label ?? '').includes(value)
}

/** 停用操作对已停用节点语义反转为"启用" */
function opLabel(op: TreeOperation, node: TreeNodeData | null): string {
  if (op === 'add') return node ? '新增子级' : '新增'
  if (op === 'edit') return '编辑'
  if (op === 'disable') return node?.disabled ? '启用' : '停用'
  return '删除'
}

function handleNodeClick(nodeData: TreeNodeData) {
  emit('node-click', nodeData)
}

// ---------- 右键菜单 ----------

const menuRef = ref<HTMLElement>()
const menu = reactive<{ visible: boolean; x: number; y: number; node: TreeNodeData | null }>({
  visible: false,
  x: 0,
  y: 0,
  node: null,
})

function openMenu(event: Event, nodeData: TreeNodeData) {
  event.preventDefault()
  // el-tree 事件签名为 Event，右键实际必为 MouseEvent
  const mouse = event as MouseEvent
  menu.node = nodeData
  menu.x = mouse.clientX
  menu.y = mouse.clientY
  menu.visible = true
}

function closeMenu() {
  menu.visible = false
  menu.node = null
}

/** 行内按钮与右键菜单共用出口，统一顺带收起菜单 */
function emitOperate(op: TreeOperation, node: TreeNodeData | null) {
  emit('node-operate', op, node)
  closeMenu()
}

function handleMenuDelete() {
  if (menu.node) emit('node-operate', 'delete', menu.node)
  closeMenu()
}

function handleDocumentClick(event: MouseEvent) {
  if (!menu.visible) return
  const target = event.target as HTMLElement | null
  // 菜单内部与 popconfirm 弹层（teleport 到 body）里的点击不关闭，保证删除确认可交互
  if (target && (menuRef.value?.contains(target) || target.closest('.el-popper'))) return
  closeMenu()
}

function handleDocumentContextMenu() {
  // 树节点右键已被 el-tree 阻止冒泡，能到达 document 的都在菜单外
  if (menu.visible) closeMenu()
}

onMounted(() => {
  document.addEventListener('click', handleDocumentClick)
  document.addEventListener('contextmenu', handleDocumentContextMenu)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick)
  document.removeEventListener('contextmenu', handleDocumentContextMenu)
})
</script>

<template>
  <div class="tree-manager">
    <div class="tree-manager__header">
      <span class="tree-manager__title">{{ title }}</span>
      <el-button
        v-if="operations.includes('add')"
        type="primary"
        size="small"
        :icon="Plus"
        @click="emitOperate('add', null)"
      >
        新增
      </el-button>
    </div>
    <el-input
      v-model="filterText"
      placeholder="输入名称过滤"
      :prefix-icon="Search"
      clearable
      class="tree-manager__search"
    />
    <el-tree
      ref="treeRef"
      :data="data"
      node-key="id"
      :default-expand-all="defaultExpandAll"
      :expand-on-click-node="false"
      :filter-node-method="filterNode"
      highlight-current
      @node-click="handleNodeClick"
      @node-contextmenu="openMenu"
    >
      <template #default="{ data: nodeData }">
        <span class="tree-manager__node">
          <span
            class="tree-manager__label"
            :class="{ 'tree-manager__label--disabled': nodeData.disabled }"
          >
            {{ nodeData.label }}
          </span>
          <span class="tree-manager__ops" @click.stop>
            <template v-for="op in operations" :key="op">
              <el-popconfirm
                v-if="op === 'delete'"
                title="确认删除该节点？"
                width="200"
                @confirm="emitOperate('delete', nodeData as TreeNodeData)"
              >
                <template #reference>
                  <el-button link type="danger" size="small">删除</el-button>
                </template>
              </el-popconfirm>
              <el-button
                v-else
                link
                type="primary"
                size="small"
                @click="emitOperate(op, nodeData as TreeNodeData)"
              >
                {{ opLabel(op, nodeData as TreeNodeData) }}
              </el-button>
            </template>
          </span>
        </span>
      </template>
    </el-tree>

    <!-- 右键菜单：fixed 定位跟随鼠标，点击其他区域关闭 -->
    <div
      v-if="menu.visible"
      ref="menuRef"
      class="tree-manager__menu"
      :style="{ left: `${menu.x}px`, top: `${menu.y}px` }"
    >
      <template v-for="op in operations" :key="op">
        <el-popconfirm
          v-if="op === 'delete'"
          title="确认删除该节点？"
          width="200"
          @confirm="handleMenuDelete"
          @cancel="closeMenu"
        >
          <template #reference>
            <div class="tree-manager__menu-item tree-manager__menu-item--danger">删除</div>
          </template>
        </el-popconfirm>
        <div v-else class="tree-manager__menu-item" @click="emitOperate(op, menu.node)">
          {{ opLabel(op, menu.node) }}
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.tree-manager__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.tree-manager__title {
  font-size: 15px;
  font-weight: 600;
}

.tree-manager__search {
  margin-bottom: 8px;
}

.tree-manager__node {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: space-between;
  min-width: 0;
  padding-right: 8px;
}

.tree-manager__label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tree-manager__label--disabled {
  color: var(--el-text-color-placeholder);
}

/* 操作按钮仅悬停时出现，避免树形界面按钮噪音 */
.tree-manager__ops {
  display: flex;
  flex-shrink: 0;
  gap: 2px;
  visibility: hidden;
}

.tree-manager__node:hover .tree-manager__ops {
  visibility: visible;
}

.tree-manager__menu {
  position: fixed;
  z-index: 3000;
  min-width: 110px;
  padding: 4px;
  background: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  box-shadow: var(--el-box-shadow-light);
}

.tree-manager__menu-item {
  padding: 6px 12px;
  font-size: 13px;
  cursor: pointer;
  border-radius: 3px;
}

.tree-manager__menu-item:hover {
  background: var(--el-fill-color-light);
}

.tree-manager__menu-item--danger {
  color: var(--el-color-danger);
}
</style>
