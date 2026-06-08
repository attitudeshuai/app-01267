<template>
  <div class="page-container">
    <div class="page-header">
      <h2>操作日志</h2>
    </div>
    <div class="card-wrapper">
      <div class="filter-bar">
        <el-select v-model="query.module" placeholder="模块" clearable style="width: 140px" @change="fetchData">
          <el-option label="Admin" value="Admin" />
          <el-option label="Merchant" value="Merchant" />
          <el-option label="Purchaser" value="Purchaser" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="搜索操作人/操作" clearable style="width: 200px" @keyup.enter="fetchData" />
        <el-button type="primary" @click="fetchData">搜索</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="operatorType" label="操作人类型" width="100">
          <template #default="{ row }">{{ operatorTypeLabel(row.operatorType) }}</template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="120" />
        <el-table-column prop="module" label="模块" width="100" />
        <el-table-column prop="action" label="操作" min-width="150" />
        <el-table-column prop="detail" label="详情" min-width="180" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column prop="createdTime" label="时间" width="170" />
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getOperationLogs } from '../api/admin'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, module: null, keyword: null })

function operatorTypeLabel(type) {
  const map = { 1: '管理员', 2: '商户', 3: '采购商' }
  return map[type] || '未知'
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getOperationLogs(query)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>
