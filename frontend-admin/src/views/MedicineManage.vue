<template>
  <div class="page-container">
    <div class="page-header">
      <h2>药材管理</h2>
      <el-button type="primary" @click="openDialog(null)">新增药材</el-button>
    </div>
    <div class="card-wrapper">
      <div class="filter-bar">
        <el-input v-model="query.keyword" placeholder="搜索药材名称" clearable style="width: 200px" @clear="fetchData" @keyup.enter="fetchData" />
        <el-select v-model="query.categoryId" placeholder="选择分类" clearable style="width: 160px" @change="fetchData">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态筛选" clearable style="width: 140px" @change="fetchData">
          <el-option label="待审核" :value="0" />
          <el-option label="已上架" :value="1" />
          <el-option label="已下架" :value="2" />
          <el-option label="已拒绝" :value="3" />
        </el-select>
        <el-button type="primary" @click="fetchData">搜索</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="name" label="药材名称" min-width="140" />
        <el-table-column prop="categoryName" label="分类" width="100" />
        <el-table-column prop="origin" label="产地" width="100" />
        <el-table-column prop="price" label="价格" width="100">
          <template #default="{ row }">¥{{ (row.price || 0).toFixed(2) }}/{{ row.unit || '克' }}</template>
        </el-table-column>
        <el-table-column prop="stockQuantity" label="库存" width="80" />
        <el-table-column prop="salesCount" label="销量" width="80" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="medicineStatusTag(row.status).type">{{ medicineStatusTag(row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="340" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="openDialog(row)">编辑</el-button>
            <el-button v-if="row.status === 0" type="success" size="small" @click="handleAudit(row, 1)">通过</el-button>
            <el-button v-if="row.status === 0" type="danger" size="small" @click="handleAudit(row, 3)">拒绝</el-button>
            <el-button v-if="row.status === 1" type="warning" size="small" @click="handleAudit(row, 2)">下架</el-button>
            <el-button v-if="row.status === 2 || row.status === 3" type="success" size="small" @click="handleAudit(row, 1)">上架</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑药材' : '新增药材'" width="680px" destroy-on-close @closed="() => document.activeElement?.blur?.()">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="药材名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入药材名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类" prop="categoryId">
              <el-select v-model="form.categoryId" placeholder="选择分类" style="width: 100%">
                <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="产地" prop="origin">
              <el-input v-model="form.origin" placeholder="如：云南文山" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="规格">
              <el-input v-model="form.specification" placeholder="如：20头、切片" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="单价" prop="price">
              <el-input-number v-model="form.price" :min="0" :max="999999" :precision="2" :step="0.1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单位">
              <el-select v-model="form.unit" style="width: 100%">
                <el-option label="克" value="克" />
                <el-option label="千克" value="千克" />
                <el-option label="包" value="包" />
                <el-option label="盒" value="盒" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="所属商户" prop="merchantId">
              <el-select v-model="form.merchantId" placeholder="选择商户" style="width: 100%" filterable>
                <el-option v-for="m in merchants" :key="m.id" :label="`${m.companyName || m.username} (${m.username})`" :value="m.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="库存" prop="stockQuantity">
              <el-input-number v-model="form.stockQuantity" :min="0" style="width: 100%" placeholder="当前库存数量" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预警阈值">
              <el-input-number v-model="form.warningThreshold" :min="0" style="width: 100%" placeholder="低于此值触发预警" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="图片">
          <div class="upload-area">
            <el-upload :show-file-list="false" :http-request="handleImageUpload" accept="image/*">
              <el-button type="primary" size="small">
                <el-icon style="margin-right: 4px"><Upload /></el-icon>
                {{ form.image ? '重新上传' : '上传图片' }}
              </el-button>
            </el-upload>
            <div v-if="form.image" class="preview-wrapper">
              <el-image :src="form.image" style="width: 120px; height: 80px; border-radius: 6px; cursor: pointer;" fit="cover" :preview-src-list="[form.image]" preview-teleported />
              <el-button type="danger" size="small" circle style="position: absolute; top: -6px; right: -6px;" @click.stop="form.image = ''">
                <el-icon><Close /></el-icon>
              </el-button>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="药材描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="药材功效、适用症状等" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="质量等级">
              <el-select v-model="form.qualityGrade" placeholder="选择等级" style="width: 100%">
                <el-option label="特级" value="特级" />
                <el-option label="一级" value="一级" />
                <el-option label="二级" value="二级" />
                <el-option label="三级" value="三级" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="待审核" :value="0" />
                <el-option label="已上架" :value="1" />
                <el-option label="已下架" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Close } from '@element-plus/icons-vue'
import { getMedicines, createMedicine, updateMedicine, deleteMedicine, auditMedicine, getAllCategories, getMerchants, uploadFile } from '../api/admin'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref([])
const total = ref(0)
const categories = ref([])
const merchants = ref([])
const query = reactive({ current: 1, size: 10, keyword: '', categoryId: '', status: '' })
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const form = reactive({
  id: null,
  name: '',
  categoryId: null,
  origin: '',
  specification: '',
  qualityGrade: '一级',
  unit: '克',
  price: 0,
  merchantId: 1,
  stockQuantity: null,
  warningThreshold: null,
  image: '',
  description: '',
  status: 1
})

const rules = {
  name: [{ required: true, message: '请输入药材名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  origin: [{ required: true, message: '请输入产地', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  merchantId: [{ required: true, message: '请选择所属商户', trigger: 'change' }]
}

function medicineStatusTag(s) {
  const map = {
    0: { label: '待审核', type: 'warning' },
    1: { label: '已上架', type: 'success' },
    2: { label: '已下架', type: 'info' },
    3: { label: '已拒绝', type: 'danger' }
  }
  return map[s] || { label: '未知', type: 'info' }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getMedicines(query)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    const res = await getAllCategories()
    categories.value = res.data || []
  } catch { /* ignore */ }
}

async function handleImageUpload({ file }) {
  try {
    const res = await uploadFile(file)
    form.image = res.data || ''
    ElMessage.success('上传成功')
  } catch { /* handled */ }
}

async function loadMerchants() {
  try {
    const res = await getMerchants({ current: 1, size: 200 })
    merchants.value = res.data?.records || []
  } catch { /* ignore */ }
}

function openDialog(row) {
  isEdit.value = !!row
  if (!merchants.value.length) loadMerchants()
  if (row) {
    Object.assign(form, {
      id: row.id,
      name: row.name,
      categoryId: row.categoryId,
      origin: row.origin || '',
      specification: row.specification || '',
      qualityGrade: row.qualityGrade || '一级',
      unit: row.unit || '克',
      price: row.price || 0,
      merchantId: row.merchantId,
      stockQuantity: row.stockQuantity ?? null,
      warningThreshold: row.warningThreshold ?? null,
      image: row.image || '',
      description: row.description || '',
      status: row.status ?? 1
    })
  } else {
    Object.assign(form, {
      id: null,
      name: '',
      categoryId: null,
      origin: '',
      specification: '',
      qualityGrade: '一级',
      unit: '克',
      price: 0,
      merchantId: merchants.value.length ? merchants.value[0].id : null,
      stockQuantity: null,
      warningThreshold: null,
      image: '',
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
      await updateMedicine({ ...form })
    } else {
      await createMedicine({ ...form })
    }
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    fetchData()
  } finally {
    submitLoading.value = false
  }
}

async function handleAudit(row, status) {
  const labels = { 1: '上架', 2: '下架', 3: '拒绝' }
  const label = labels[status] || '操作'
  try {
    await ElMessageBox.confirm(`确定${label}药材「${row.name}」吗？`, '确认', { type: 'warning' })
    await auditMedicine(row.id, status)
    ElMessage.success(`已${label}`)
    fetchData()
  } catch { /* cancelled */ }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除药材「${row.name}」吗？此操作不可恢复！`, '警告', { type: 'error' })
    await deleteMedicine(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch { /* cancelled */ }
}

onMounted(() => {
  fetchData()
  loadCategories()
  loadMerchants()
})
</script>

<style scoped>
.upload-area { display: flex; flex-direction: column; gap: 8px; }
.preview-wrapper { position: relative; display: inline-block; margin-top: 4px; }
</style>
