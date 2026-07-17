<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { MOCK_ACCOUNT, useUserStore } from '@/stores/user'

defineOptions({ name: 'LoginView' })

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  userNo: 'admin',
  password: '',
})

const rules: FormRules = {
  userNo: [{ required: true, message: '请输入工号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    await userStore.login({ userNo: form.userNo, password: form.password })
    ElMessage.success(`欢迎，${userStore.userName}`)
    const redirect = route.query.redirect
    router.push(typeof redirect === 'string' ? redirect : '/')
  } catch {
    // 错误提示由 request 拦截器统一弹出
  } finally {
    loading.value = false
  }
}

function fillDemo() {
  form.userNo = MOCK_ACCOUNT.userNo
  form.password = MOCK_ACCOUNT.password
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
        <el-form-item prop="userNo">
          <el-input v-model="form.userNo" placeholder="工号" :prefix-icon="User" />
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
      <div class="login-page__tip">
        <div>内置管理员：admin / admin123（首次登录后请修改密码）</div>
        <div>
          演示账号：demo / demo（不连后端预览页面，
          <el-link type="primary" :underline="false" class="login-page__demo" @click="fillDemo">
            一键填入
          </el-link>
          ）
        </div>
      </div>
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

.login-page__submit {
  width: 100%;
}

.login-page__tip {
  margin-top: 16px;
  font-size: 12px;
  line-height: 20px;
  color: var(--el-text-color-placeholder);
  text-align: center;
}

.login-page__demo {
  font-size: 12px;
  vertical-align: baseline;
}
</style>
