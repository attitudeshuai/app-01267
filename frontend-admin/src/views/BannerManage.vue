<template>
  <div class="page-container">
    <div class="page-header">
      <h2>轮播图管理</h2>
      <el-button type="primary" @click="openDialog(null)">新增轮播图</el-button>
    </div>
    <div class="card-wrapper">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="title" label="标题" min-width="160" />
        <el-table-column prop="image" label="图片" width="160">
          <template #default="{ row }">
            <el-image 
              v-if="row.image" 
              :src="row.image" 
              style="width: 120px; height: 60px; border-radius: 4px; cursor: pointer;" 
              fit="cover" 
              :preview-src-list="[row.image]"
              preview-teleported
            >
              <template #placeholder>
                <div class="image-loading">加载中...</div>
              </template>
              <template #error>
                <div class="image-error">加载失败</div>
              </template>
            </el-image>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="linkUrl" label="跳转链接" min-width="120">
          <template #default="{ row }">{{ row.linkUrl || '-' }}</template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="openDialog(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑轮播图' : '新增轮播图'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="图片" prop="image">
          <div class="upload-area">
            <el-upload
              :show-file-list="false"
              :http-request="handleUpload"
              accept="image/*"
            >
              <el-button type="primary">
                <el-icon style="margin-right: 4px"><Upload /></el-icon>
                {{ form.image ? '重新上传' : '上传图片' }}
              </el-button>
            </el-upload>
            <div v-if="form.image" class="preview-wrapper">
              <el-image 
                :src="form.image" 
                style="width: 280px; height: 120px; border-radius: 8px;" 
                fit="cover" 
                :preview-src-list="[form.image]"
                preview-teleported
              >
                <template #placeholder>
                  <div class="image-loading">加载中...</div>
                </template>
              </el-image>
              <el-button type="danger" size="small" circle style="position: absolute; top: -8px; right: -8px;" @click="form.image = ''">
                <el-icon><Close /></el-icon>
              </el-button>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="点击跳转">
          <div class="link-config">
            <el-select v-model="linkType" placeholder="选择跳转类型" clearable style="width: 140px" @change="onLinkTypeChange">
              <el-option label="不跳转" value="" />
              <el-option label="首页" value="/" />
              <el-option label="药材中心" value="/medicines" />
              <el-option label="商户入驻" value="/register/merchant" />
              <el-option label="采购商注册" value="/register/purchaser" />
              <el-option label="自定义链接" value="custom" />
            </el-select>
            <el-input
              v-if="linkType === 'custom'"
              v-model="form.linkUrl"
              placeholder="输入路径如 /medicines 或 /medicines/26（商品详情）"
              style="flex: 1; margin-left: 12px"
            />
            <span v-else-if="linkType && linkType !== 'custom'" class="link-preview">{{ linkType }}</span>
          </div>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
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
import { getBanners, createBanner, updateBanner, deleteBanner, uploadFile } from '../api/admin'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const form = reactive({ id: null, title: '', image: '', linkUrl: '', sortOrder: 0, status: 1 })
const linkType = ref('')
const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  image: [{ required: true, message: '请上传图片', trigger: 'change' }]
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getBanners()
    tableData.value = res.data?.records || res.data || []
  } finally {
    loading.value = false
  }
}

const PRESET_LINKS = ['', '/', '/medicines', '/register/merchant', '/register/purchaser']

function openDialog(row) {
  isEdit.value = !!row
  if (row) {
    Object.assign(form, { id: row.id, title: row.title, image: row.image || '', linkUrl: row.linkUrl || '', sortOrder: row.sortOrder || 0, status: row.status ?? 1 })
    linkType.value = PRESET_LINKS.includes(row.linkUrl || '') ? (row.linkUrl || '') : (row.linkUrl ? 'custom' : '')
  } else {
    Object.assign(form, { id: null, title: '', image: '', linkUrl: '', sortOrder: 0, status: 1 })
    linkType.value = ''
  }
  dialogVisible.value = true
}

function onLinkTypeChange(val) {
  if (val && val !== 'custom') {
    form.linkUrl = val
  } else if (val !== 'custom') {
    form.linkUrl = ''
  }
}

async function handleUpload({ file }) {
  try {
    const res = await uploadFile(file)
    form.image = res.data || ''
    ElMessage.success('上传成功')
  } catch { /* handled */ }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (linkType.value && linkType.value !== 'custom') {
    form.linkUrl = linkType.value
  }
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateBanner({ ...form })
    } else {
      await createBanner({ ...form })
    }
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    fetchData()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除轮播图「${row.title}」吗？`, '提示', { type: 'warning' })
    await deleteBanner(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch { /* cancelled */ }
}

onMounted(fetchData)
</script>

<style scoped>
.upload-area {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.preview-wrapper {
  position: relative;
  display: inline-block;
  margin-top: 4px;
}
.image-loading,
.image-error {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #909399;
  font-size: 12px;
}
.link-config {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.link-preview {
  font-size: 13px;
  color: #606266;
  margin-left: 12px;
}
</style>
