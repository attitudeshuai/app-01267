<template>
  <div class="profile-page">
    <div class="page-header"><h2>个人信息</h2></div>
    <div class="profile-card" v-loading="loading">
      <div class="avatar-section">
        <el-avatar :size="80" :style="{ backgroundColor: '#FF7A45', fontSize: '32px' }">
          {{ (form.nickname || 'U').charAt(0).toUpperCase() }}
        </el-avatar>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width: 480px">
        <el-form-item label="用户名">
          <el-input :value="form.username" disabled />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave" round>保存修改</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '../../store/user'
import { getProfile, updateProfile } from '../../api/purchaser'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const saving = ref(false)
const form = reactive({ username: '', nickname: '', phone: '', email: '', avatar: '' })

const rules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getProfile()
    Object.assign(form, res.data)
  } catch (_) { /* error handled */ }
  loading.value = false
})

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await updateProfile(form)
    ElMessage.success('保存成功')
    userStore.setUserInfo({ nickname: form.nickname, phone: form.phone, email: form.email })
  } catch (_) { /* error handled */ }
  saving.value = false
}
</script>

<style scoped>
.page-header { margin-bottom: 20px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #333; }
.profile-card {
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.avatar-section {
  display: flex;
  justify-content: center;
  margin-bottom: 32px;
}
</style>
