<template>
  <div class="merchant-profile-page">
    <div class="page-header"><h2>商家信息</h2></div>
    <div class="profile-card" v-loading="loading">
      <div class="avatar-section">
        <el-avatar :size="80" :style="{ backgroundColor: '#FF7A45', fontSize: '32px' }">
          {{ (form.companyName || 'M').charAt(0).toUpperCase() }}
        </el-avatar>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width: 520px">
        <el-form-item label="用户名">
          <el-input :value="form.username" disabled />
        </el-form-item>
        <el-form-item label="公司名称" prop="companyName">
          <el-input v-model="form.companyName" placeholder="请输入公司名称" />
        </el-form-item>
        <el-form-item label="联系人" prop="contactPerson">
          <el-input v-model="form.contactPerson" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="营业执照号">
          <el-input v-model="form.licenseNo" placeholder="营业执照号" />
        </el-form-item>
        <el-form-item label="公司描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="公司描述" />
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
import { getProfile, updateProfile } from '../../api/merchant'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const saving = ref(false)
const form = reactive({
  username: '', companyName: '', contactPerson: '', phone: '', email: '', licenseNo: '', description: ''
})

const rules = {
  companyName: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
  contactPerson: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
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
    userStore.setUserInfo({ companyName: form.companyName })
  } catch (_) { /* error handled */ }
  saving.value = false
}
</script>

<style scoped>
.page-header { margin-bottom: 20px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #333; }
.profile-card {
  background: #fff; border-radius: 12px; padding: 32px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.avatar-section { display: flex; justify-content: center; margin-bottom: 32px; }
</style>
