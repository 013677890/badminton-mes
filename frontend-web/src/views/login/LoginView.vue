<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

defineOptions({ name: 'LoginView' })

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  userName: 'admin',
  password: '123456',
  roleCodes: ['ADMIN'] as string[],
})

/** 演示用角色选择；接入认证模块后角色由后端返回 */
const roleOptions = [
  { label: '系统管理员（ADMIN）', value: 'ADMIN' },
  { label: '生产计划员（PLANNER）', value: 'PLANNER' },
  { label: '车间主任（WORKSHOP_MANAGER）', value: 'WORKSHOP_MANAGER' },
  { label: '质检员（INSPECTOR）', value: 'INSPECTOR' },
  { label: '操作工（OPERATOR）', value: 'OPERATOR' },
]

const rules: FormRules = {
  userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  roleCodes: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

async function handleLogin() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    userStore.login({ userName: form.userName, roleCodes: form.roleCodes })
    ElMessage.success('登录成功')
    const redirect = route.query.redirect
    router.push(typeof redirect === 'string' ? redirect : '/')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <el-card class="login-page__card" shadow="always">
      <div class="login-page__title">羽毛球 MES</div>
      <div class="login-page__subtitle">生产制造执行系统 · 管理后台</div>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="0"
        size="large"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="userName">
          <el-input v-model="form.userName" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            show-password
            :prefix-icon="Lock"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item prop="roleCodes">
          <el-select
            v-model="form.roleCodes"
            multiple
            collapse-tags
            placeholder="选择演示角色（影响菜单与按钮权限）"
            class="login-page__roles"
          >
            <el-option
              v-for="opt in roleOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-button
          type="primary"
          size="large"
          class="login-page__submit"
          :loading="loading"
          @click="handleLogin"
        >
          登 录
        </el-button>
      </el-form>
      <div class="login-page__tip">演示登录：本地模拟，未接认证接口</div>
    </el-card>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: linear-gradient(135deg, #001529 0%, #003a70 100%);
}

.login-page__card {
  width: 380px;
  padding: 8px 12px;
}

.login-page__title {
  font-size: 24px;
  font-weight: 700;
  text-align: center;
}

.login-page__subtitle {
  margin: 6px 0 24px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  text-align: center;
}

.login-page__roles,
.login-page__submit {
  width: 100%;
}

.login-page__tip {
  margin-top: 16px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  text-align: center;
}
</style>
