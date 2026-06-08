<template>
  <div class="login-page">
    <div class="login-card">
      <h2 class="login-title">欢迎登录</h2>
      <el-tabs v-model="activeRole" class="login-tabs" stretch>
        <el-tab-pane label="采购商登录" name="PURCHASER" />
        <el-tab-pane label="商家登录" name="MERCHANT" />
      </el-tabs>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" size="large" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" style="width:100%" @click="handleLogin" round>登 录</el-button>
        </el-form-item>
      </el-form>
      <div class="login-footer">
        <span>还没有账号？</span>
        <router-link v-if="activeRole === 'PURCHASER'" to="/register/purchaser" class="reg-link">注册采购商</router-link>
        <router-link v-else to="/register/merchant" class="reg-link">注册商家</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../store/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const activeRole = ref('PURCHASER')

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.login({ ...form, role: activeRole.value })
    ElMessage.success('登录成功')
    const redirect = route.query.redirect
    if (redirect) {
      router.push(redirect)
    } else if (activeRole.value === 'MERCHANT') {
      router.push('/merchant/dashboard')
    } else {
      router.push('/')
    }
  } catch (_) { /* error handled in interceptor */ }
  loading.value = false
}
</script>

<style scoped>
.login-page {
  min-height: calc(100vh - 170px);
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #fff5f0, #F5F7FA);
}
.login-card {
  width: 420px;
  background: #fff;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
}
.login-title {
  text-align: center;
  font-size: 24px;
  color: #333;
  margin-bottom: 24px;
}
.login-tabs {
  margin-bottom: 24px;
}
.login-footer {
  text-align: center;
  font-size: 14px;
  color: #8C8C8C;
}
.reg-link {
  color: #FF7A45;
  margin-left: 4px;
}
.reg-link:hover {
  text-decoration: underline;
}
</style>
