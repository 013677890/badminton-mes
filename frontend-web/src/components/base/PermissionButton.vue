<script setup lang="ts">
import { computed } from 'vue'
import { usePermission } from '@/composables/usePermission'

defineOptions({ name: 'PermissionButton', inheritAttrs: false })

const props = withDefaults(
  defineProps<{
    /** 需要的角色编码，不配则所有人可用 */
    roles?: string[]
    /** hide=无权限隐藏（默认）；disable=无权限置灰并提示 */
    mode?: 'hide' | 'disable'
    disabled?: boolean
  }>(),
  { mode: 'hide' },
)

const { hasRole } = usePermission()

const allowed = computed(() => hasRole(props.roles))
const visible = computed(() => (props.mode === 'hide' ? allowed.value : true))
</script>

<template>
  <el-tooltip v-if="visible && !allowed" content="无操作权限" placement="top">
    <!-- disabled 按钮不触发事件，需外层 span 承接 tooltip -->
    <span class="permission-button__disabled-wrap">
      <el-button v-bind="$attrs" disabled>
        <slot />
      </el-button>
    </span>
  </el-tooltip>
  <el-button v-else-if="visible" v-bind="$attrs" :disabled="disabled">
    <slot />
  </el-button>
</template>

<style scoped>
.permission-button__disabled-wrap {
  display: inline-block;
}
</style>
