<template>
  <div class="page-container">
    <div class="page-header"><h2>系统配置</h2></div>
    <div class="card-wrapper">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="configKey" label="配置键" min-width="180" />
        <el-table-column prop="configValue" label="配置值" min-width="220">
          <template #default="{ row }">
            <template v-if="editingId === row.id">
              <el-input v-model="editValue" size="small" style="width: 200px" @keyup.enter="handleSave(row)" />
            </template>
            <span v-else>{{ row.configValue }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <template v-if="editingId === row.id">
              <el-button type="success" size="small" :loading="saveLoading" @click="handleSave(row)">保存</el-button>
              <el-button size="small" @click="cancelEdit">取消</el-button>
            </template>
            <el-button v-else type="primary" size="small" @click="startEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getConfigs, updateConfig } from '../api/admin'

const loading = ref(false)
const saveLoading = ref(false)
const tableData = ref([])
const editingId = ref(null)
const editValue = ref('')

async function fetchData() {
  loading.value = true
  try {
    const res = await getConfigs()
    tableData.value = res.data || []
  } finally {
    loading.value = false
  }
}

function startEdit(row) {
  editingId.value = row.id
  editValue.value = row.configValue
}

function cancelEdit() {
  editingId.value = null
  editValue.value = ''
}

async function handleSave(row) {
  saveLoading.value = true
  try {
    await updateConfig({ ...row, configValue: editValue.value })
    ElMessage.success('保存成功')
    editingId.value = null
    fetchData()
  } finally {
    saveLoading.value = false
  }
}

onMounted(fetchData)
</script>
