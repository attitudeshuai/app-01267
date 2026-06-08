<template>
  <div class="medicines-page">
    <div class="page-header">
      <h2>药材管理</h2>
      <el-button type="primary" @click="openDialog()" round><el-icon><Plus /></el-icon> 添加药材</el-button>
    </div>
    <div class="filter-bar">
      <el-input v-model="keyword" placeholder="搜索药材" clearable style="width:200px" @keyup.enter="handleSearch" @clear="handleSearch">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="categoryId" placeholder="全部分类" clearable @change="handleSearch" style="width:140px">
        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-select v-model="status" placeholder="全部状态" clearable @change="handleSearch" style="width:120px">
        <el-option label="待审核" :value="0" />
        <el-option label="在售" :value="1" />
        <el-option label="下架" :value="2" />
        <el-option label="已拒绝" :value="3" />
      </el-select>
    </div>

    <el-table :data="medicines" v-loading="loading" stripe style="width:100%" border round>
      <el-table-column label="药材" min-width="200">
        <template #default="{ row }">
          <div style="display:flex;align-items:center;gap:10px">
            <div style="width:48px;height:48px;border-radius:6px;overflow:hidden;flex-shrink:0;background:#fff5f0;display:flex;align-items:center;justify-content:center">
              <img v-if="row.image" :src="row.image" style="width:100%;height:100%;object-fit:cover" />
              <el-icon v-else :size="20" color="#FF7A45"><FirstAidKit /></el-icon>
            </div>
            <div>
              <div style="font-weight:600;color:#333">{{ row.name }}</div>
              <div style="font-size:12px;color:#8C8C8C">{{ row.categoryName || '' }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="price" label="价格" width="100">
        <template #default="{ row }"><span style="color:#FF7A45;font-weight:600">¥{{ row.price }}</span></template>
      </el-table-column>
      <el-table-column prop="stockQuantity" label="库存" width="80" />
      <el-table-column prop="salesCount" label="销量" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="medicineStatusTag(row.status).type" size="small">{{ medicineStatusTag(row.status).label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" size="small" @click="openDialog(row)">编辑</el-button>
          <el-button v-if="row.status === 2 || row.status === 3" text type="success" size="small" @click="handleOnShelf(row)">上架</el-button>
          <el-button v-else-if="row.status === 1" text type="warning" size="small" @click="handleOffShelf(row)">下架</el-button>
          <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="total > 0" class="pagination-wrap">
      <el-pagination v-model:current-page="current" :page-size="size" :total="total" layout="total, prev, pager, next" background @current-change="fetchMedicines" />
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑药材' : '添加药材'" width="640px" top="5vh" @closed="() => document.activeElement?.blur?.()">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入药材名称" />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width:100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:0 16px">
          <el-form-item label="产地" prop="origin">
            <el-input v-model="form.origin" placeholder="产地" />
          </el-form-item>
          <el-form-item label="品质等级" prop="qualityGrade">
            <el-input v-model="form.qualityGrade" placeholder="品质等级" />
          </el-form-item>
          <el-form-item label="规格" prop="specification">
            <el-input v-model="form.specification" placeholder="规格" />
          </el-form-item>
          <el-form-item label="单位" prop="unit">
            <el-input v-model="form.unit" placeholder="如: 克、包、份" />
          </el-form-item>
          <el-form-item label="价格" prop="price">
            <el-input-number v-model="form.price" :min="0" :max="999999" :precision="2" style="width:100%" />
          </el-form-item>
          <el-form-item label="库存" prop="stockQuantity">
            <el-input-number v-model="form.stockQuantity" :min="0" style="width:100%" />
          </el-form-item>
        </div>
        <el-form-item label="预警阈值">
          <el-input-number v-model="form.warningThreshold" :min="0" style="width:200px" />
        </el-form-item>
        <el-form-item label="主图">
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
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="药材描述" />
        </el-form-item>
        <el-form-item label="溯源信息">
          <el-input v-model="form.traceInfo" type="textarea" :rows="2" placeholder="溯源信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Upload, Close } from '@element-plus/icons-vue'
import { getMedicines, addMedicine, updateMedicine, onShelfMedicine, offShelfMedicine, deleteMedicine } from '../../api/merchant'
import { getCategories, uploadFile } from '../../api/common'
import { ElMessage, ElMessageBox } from 'element-plus'

const medicines = ref([])
const categories = ref([])
const keyword = ref('')
const categoryId = ref('')
const status = ref('')
const current = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const defaultForm = {
  id: null, name: '', categoryId: '', origin: '', qualityGrade: '', specification: '',
  unit: '', price: 0, image: '', images: '', description: '', traceInfo: '',
  stockQuantity: 0, warningThreshold: 10
}
const form = reactive({ ...defaultForm })

async function handleImageUpload({ file }) {
  try {
    const res = await uploadFile(file)
    form.image = res.data || ''
    ElMessage.success('上传成功')
  } catch { /* handled */ }
}

const formRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  stockQuantity: [{ required: true, message: '请输入库存', trigger: 'blur' }]
}

function medicineStatusTag(s) {
  const map = { 0: { label: '待审核', type: 'warning' }, 1: { label: '在售', type: 'success' }, 2: { label: '下架', type: 'info' }, 3: { label: '已拒绝', type: 'danger' } }
  return map[s] ?? { label: '未知', type: 'info' }
}

onMounted(() => {
  fetchCategories()
  fetchMedicines()
})

async function fetchCategories() {
  try {
    const res = await getCategories()
    categories.value = res.data || []
  } catch (_) { /* ignore */ }
}

async function fetchMedicines() {
  loading.value = true
  try {
    const res = await getMedicines({ current: current.value, size: size.value, keyword: keyword.value, categoryId: categoryId.value || undefined, status: (status.value === '' || status.value == null) ? undefined : status.value })
    const data = res.data || {}
    medicines.value = data.records || data || []
    total.value = data.total || 0
  } catch (_) { medicines.value = [] }
  loading.value = false
}

function handleSearch() { current.value = 1; fetchMedicines() }

function openDialog(row) {
  if (row) {
    isEdit.value = true
    Object.assign(form, row)
  } else {
    isEdit.value = false
    Object.assign(form, defaultForm)
  }
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (isEdit.value) {
      await updateMedicine({ ...form })
    } else {
      await addMedicine({ ...form })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchMedicines()
  } catch (_) { /* error handled */ }
  saving.value = false
}

async function handleOnShelf(row) {
  try {
    await onShelfMedicine(row.id)
    ElMessage.success('已上架')
    fetchMedicines()
  } catch (_) { /* error handled */ }
}

async function handleOffShelf(row) {
  try {
    await ElMessageBox.confirm('确定要下架该药材吗？', '提示', { type: 'warning' })
    await offShelfMedicine(row.id)
    ElMessage.success('已下架')
    fetchMedicines()
  } catch (_) { /* cancelled */ }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定要删除该药材吗？此操作不可恢复。', '警告', { type: 'error' })
    await deleteMedicine(row.id)
    ElMessage.success('已删除')
    fetchMedicines()
  } catch (_) { /* cancelled */ }
}
</script>

<style scoped>
.upload-area { display: flex; flex-direction: column; gap: 8px; }
.preview-wrapper { position: relative; display: inline-block; margin-top: 4px; }
.page-header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;
}
.page-header h2 { font-size: 20px; font-weight: 600; color: #333; }
.filter-bar {
  display: flex; gap: 12px; margin-bottom: 16px;
  background: #fff; border-radius: 8px; padding: 12px 16px;
}
.pagination-wrap { display: flex; justify-content: center; margin-top: 20px; }
</style>
