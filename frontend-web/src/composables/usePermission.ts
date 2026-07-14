import { computed } from 'vue'
import { ADMIN_ROLE, useUserStore } from '@/stores/user'

/**
 * 角色权限判断（菜单/按钮显隐）。
 * 前端权限仅用于交互降噪，后端 @RequiresRoles 是最终防线。
 */
export function usePermission() {
  const userStore = useUserStore()

  const roleCodes = computed(() => userStore.roleCodes)

  /** 不传或空数组视为放行；ADMIN 拥有全部权限；命中任一角色即通过 */
  function hasRole(roles?: string[] | string): boolean {
    if (!roles || roles.length === 0) return true
    const required = Array.isArray(roles) ? roles : [roles]
    if (required.length === 0) return true
    const owned = userStore.roleCodes
    if (owned.includes(ADMIN_ROLE)) return true
    return required.some((role) => owned.includes(role))
  }

  return { roleCodes, hasRole }
}
