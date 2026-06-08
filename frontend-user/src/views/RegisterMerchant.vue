<template>
  <div class="register-page">
    <div class="register-card">
      <h2 class="register-title">商家注册</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" size="large" @submit.prevent="handleRegister">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" show-password />
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
        <el-form-item label="营业执照号" prop="licenseNo">
          <el-input v-model="form.licenseNo" placeholder="请输入营业执照号" />
        </el-form-item>
        <el-form-item label="营业执照图片">
          <el-upload :show-file-list="false" :http-request="handleLicenseUpload" accept="image/*">
            <el-button type="primary" size="small" v-if="!form.licenseImage">上传营业执照</el-button>
            <div v-else class="upload-preview"><img :src="imgUrl(form.licenseImage)" /><span @click.stop="form.licenseImage=''">移除</span></div>
          </el-upload>
        </el-form-item>
        <el-form-item label="经营资质图片">
          <el-upload :show-file-list="false" :http-request="handleQualificationUpload" accept="image/*">
            <el-button type="primary" size="small" v-if="!form.qualificationImage">上传经营资质</el-button>
            <div v-else class="upload-preview"><img :src="imgUrl(form.qualificationImage)" /><span @click.stop="form.qualificationImage=''">移除</span></div>
          </el-upload>
        </el-form-item>
        <el-form-item label="公司描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入公司描述" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" style="width:100%" @click="handleRegister" round>注 册</el-button>
        </el-form-item>
      </el-form>
      <div class="register-footer">
        已有账号？<router-link to="/login" class="login-link">去登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { registerMerchant } from '../api/auth'
import { uploadFile } from '../api/common'
import { ElMessage } from 'element-plus'

function imgUrl(path) {
  if (!path) return ''
  if (path.startsWith('http')) return path
  return path.startsWith('/') ? path : '/' + path
}

async function handleLicenseUpload({ file }) {
  try {
    const res = await uploadFile(file)
    form.licenseImage = res.data || ''
  } catch { /* handled */ }
}

async function handleQualificationUpload({ file }) {
  try {
    const res = await uploadFile(file)
    form.qualificationImage = res.data || ''
  } catch { /* handled */ }
}

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  companyName: '',
  contactPerson: '',
  phone: '',
  email: '',
  licenseNo: '',
  licenseImage: '',
  qualificationImage: '',
  description: ''
})

const validateConfirm = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
  { required: true, message: '请输入用户名', trigger: 'blur' },
  { min: 4, max: 20, message: '用户名长度为4-20个字符', trigger: 'blur' },
  { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线，不能有中文或特殊字符', trigger: 'blur' }
],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码至少6个字符', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请确认密码', trigger: 'blur' }, { validator: validateConfirm, trigger: 'blur' }],
  companyName: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
  contactPerson: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }, { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  licenseNo: [{ required: true, message: '请输入营业执照号', trigger: 'blur' }]
}

async function handleRegister() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const { confirmPassword, ...data } = form
    await registerMerchant({ ...data, licenseImage: form.licenseImage || undefined, qualificationImage: form.qualificationImage || undefined })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (_) { /* error handled in interceptor */ }
  loading.value = false
}
</script>

<style scoped>
.register-page {
  min-height: calc(100vh - 170px);
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #fff5f0, #F5F7FA);
  padding: 40px 0;
}
.register-card {
  width: 560px;
  background: #fff;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
}
.register-title {
  text-align: center;
  font-size: 24px;
  color: #333;
  margin-bottom: 32px;
}
.register-footer {
  text-align: center;
  font-size: 14px;
  color: #8C8C8C;
}
.login-link {
  color: #FF7A45;
}
.login-link:hover {
  text-decoration: underline;
}
.upload-preview {
  display: flex;
  align-items: center;
  gap: 8px;
}
.upload-preview img {
  max-width: 120px;
  max-height: 80px;
  object-fit: contain;
}
.upload-preview span {
  color: #F5222D;
  cursor: pointer;
  font-size: 12px;
}
</style>
