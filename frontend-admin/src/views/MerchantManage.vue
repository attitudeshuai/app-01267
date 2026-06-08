<template>
  <div class="page-container">
    <div class="page-header">
      <h2>商家管理</h2>
      <el-button type="primary" @click="openDialog(null)">新增商家</el-button>
    </div>
    <div class="card-wrapper">
      <div class="filter-bar">
        <el-input v-model="query.keyword" placeholder="搜索公司名/用户名" clearable style="width: 240px" @clear="fetchData" @keyup.enter="fetchData" />
        <el-select v-model="query.status" placeholder="状态筛选" clearable style="width: 160px" @change="fetchData">
          <el-option label="待审核" :value="0" />
          <el-option label="已通过" :value="1" />
          <el-option label="已拒绝" :value="2" />
          <el-option label="已禁用" :value="3" />
        </el-select>
        <el-button type="primary" @click="fetchData">搜索</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="companyName" label="公司名称" min-width="160" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="contactPerson" label="联系人" width="100" />
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status).type">{{ statusTag(row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="{ row }">
            <el-button type="info" size="small" @click="openQualificationDialog(row)">查看资质</el-button>
            <el-button type="primary" size="small" @click="openDialog(row)">编辑</el-button>
            <el-button v-if="row.status === 0" type="success" size="small" @click="handleAudit(row, 1)">通过</el-button>
            <el-button v-if="row.status === 0" type="danger" size="small" @click="handleAudit(row, 2)">拒绝</el-button>
            <el-button v-if="row.status === 1" type="warning" size="small" @click="handleToggle(row, 3)">禁用</el-button>
            <el-button v-if="row.status === 3" type="success" size="small" @click="handleToggle(row, 1)">启用</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑商家' : '新增商家'" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" :disabled="isEdit" placeholder="登录用户名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="密码" :prop="isEdit ? '' : 'password'">
              <el-input v-model="form.password" type="password" :placeholder="isEdit ? '留空则不修改' : '请输入密码'" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="公司名称" prop="companyName">
          <el-input v-model="form.companyName" placeholder="公司全称" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="联系人" prop="contactPerson">
              <el-input v-model="form.contactPerson" placeholder="联系人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="电话" prop="phone">
              <el-input v-model="form.phone" placeholder="11位手机号或固话" maxlength="20" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="电子邮箱，如 example@company.com" />
        </el-form-item>
        <el-form-item label="营业执照号">
          <el-input v-model="form.licenseNo" placeholder="营业执照号（可选）" />
        </el-form-item>
        <el-form-item label="营业执照图片">
          <el-upload :show-file-list="false" :http-request="(e)=>handleFormImageUpload(e,'licenseImage')" accept="image/*">
            <el-button v-if="!form.licenseImage" type="primary" size="small">上传</el-button>
            <span v-else><el-image :src="imgUrl(form.licenseImage)" style="width:80px;height:60px" fit="contain" /><el-button type="danger" size="small" text @click="form.licenseImage=''">移除</el-button></span>
          </el-upload>
        </el-form-item>
        <el-form-item label="经营资质图片">
          <el-upload :show-file-list="false" :http-request="(e)=>handleFormImageUpload(e,'qualificationImage')" accept="image/*">
            <el-button v-if="!form.qualificationImage" type="primary" size="small">上传</el-button>
            <span v-else><el-image :src="imgUrl(form.qualificationImage)" style="width:80px;height:60px" fit="contain" /><el-button type="danger" size="small" text @click="form.qualificationImage=''">移除</el-button></span>
          </el-upload>
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="商家简介（可选）" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已拒绝" :value="2" />
            <el-option label="已禁用" :value="3" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="qualificationVisible" title="商户资质信息" width="560px" destroy-on-close>
      <el-descriptions :column="1" border v-if="currentMerchant">
        <el-descriptions-item label="公司名称">{{ currentMerchant.companyName }}</el-descriptions-item>
        <el-descriptions-item label="营业执照号">{{ currentMerchant.licenseNo || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="营业执照图片">
          <template v-if="currentMerchant.licenseImage">
            <el-image :src="imgUrl(currentMerchant.licenseImage)" style="max-width: 200px; max-height: 150px" fit="contain" :preview-src-list="[imgUrl(currentMerchant.licenseImage)]" />
          </template>
          <span v-else>未上传</span>
        </el-descriptions-item>
        <el-descriptions-item label="经营资质图片">
          <template v-if="currentMerchant.qualificationImage">
            <el-image :src="imgUrl(currentMerchant.qualificationImage)" style="max-width: 200px; max-height: 150px" fit="contain" :preview-src-list="[imgUrl(currentMerchant.qualificationImage)]" />
          </template>
          <span v-else>未上传</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMerchants, createMerchant, updateMerchant, auditMerchant, toggleMerchantStatus, uploadFile } from '../api/admin'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, status: '', keyword: '' })
const dialogVisible = ref(false)
const qualificationVisible = ref(false)
const currentMerchant = ref(null)
const isEdit = ref(false)
const formRef = ref(null)

function imgUrl(path) {
  if (!path) return ''
  if (path.startsWith('http')) return path
  const base = import.meta.env.VITE_API_BASE || ''
  const origin = base ? base.replace(/\/api\/?$/, '') || window.location.origin : window.location.origin
  return origin + (path.startsWith('/') ? path : '/' + path)
}

function openQualificationDialog(row) {
  currentMerchant.value = row
  qualificationVisible.value = true
}

async function handleFormImageUpload({ file }, field) {
  try {
    const res = await uploadFile(file)
    form[field] = res.data || ''
  } catch { /* handled */ }
}

const form = reactive({
  id: null,
  username: '',
  password: '',
  companyName: '',
  contactPerson: '',
  phone: '',
  email: '',
  licenseNo: '',
  licenseImage: '',
  qualificationImage: '',
  description: '',
  status: 1
})

const phoneReg = /^1[3-9]\d{9}$|^0\d{2,3}-?\d{7,8}$/
const emailReg = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/

const rules = {
  username: [
  { required: true, message: '请输入用户名', trigger: 'blur' },
  { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线，不能有中文或特殊字符', trigger: 'blur' }
],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  companyName: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
  contactPerson: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: phoneReg, message: '请输入正确的手机号或固话格式', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { pattern: emailReg, message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

function statusTag(s) {
  const map = {
    0: { label: '待审核', type: 'warning' },
    1: { label: '已通过', type: 'success' },
    2: { label: '已拒绝', type: 'danger' },
    3: { label: '已禁用', type: 'info' }
  }
  return map[s] || { label: '未知', type: 'info' }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getMerchants(query)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  isEdit.value = !!row
  if (row) {
    Object.assign(form, {
      id: row.id,
      username: row.username,
      password: '',
      companyName: row.companyName || '',
      contactPerson: row.contactPerson || '',
      phone: row.phone || '',
      email: row.email || '',
      licenseNo: row.licenseNo || '',
      licenseImage: row.licenseImage || '',
      qualificationImage: row.qualificationImage || '',
      description: row.description || '',
      status: row.status ?? 1
    })
  } else {
    Object.assign(form, {
      id: null,
      username: '',
      password: '',
      companyName: '',
      contactPerson: '',
      phone: '',
      email: '',
      licenseNo: '',
      licenseImage: '',
      qualificationImage: '',
      description: '',
      status: 1
    })
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateMerchant({ ...form })
    } else {
      await createMerchant({ ...form })
    }
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    fetchData()
  } finally {
    submitLoading.value = false
  }
}

async function handleAudit(row, status) {
  const label = status === 1 ? '通过' : '拒绝'
  try {
    await ElMessageBox.confirm(`确定${label}商家「${row.companyName || row.username}」吗？`, '审核确认', { type: 'warning' })
    await auditMerchant(row.id, status)
    ElMessage.success(`已${label}`)
    fetchData()
  } catch { /* cancelled */ }
}

async function handleToggle(row, status) {
  const label = status === 3 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定${label}商家「${row.companyName || row.username}」吗？`, '提示', { type: 'warning' })
    await toggleMerchantStatus(row.id, status)
    ElMessage.success(`已${label}`)
    fetchData()
  } catch { /* cancelled */ }
}

onMounted(fetchData)
</script>
