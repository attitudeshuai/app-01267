<template>
  <div class="page-container">
    <div class="page-header">
      <h2>库存预警</h2>
      <el-button type="primary" @click="fetchData">刷新</el-button>
    </div>
    <div class="card-wrapper">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="name" label="药材名称" min-width="180" />
        <el-table-column prop="merchantName" label="商家" width="150" />
        <el-table-column prop="stockQuantity" label="当前库存" width="120">
          <template #default="{ row }">
            <span :style="{ color: row.stockQuantity <= 10 ? '#F5222D' : '#333' , fontWeight: row.stockQuantity <= 10 ? '600' : '400' }">{{ row.stockQuantity }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="warningThreshold" label="预警阈值" width="120" />
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column prop="categoryName" label="分类" width="120" />
      </el-table>
      <el-empty v-if="!loading && tableData.length === 0" description="暂无库存预警" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getLowStock } from '../api/admin'

const loading = ref(false)
const tableData = ref([])

async function fetchData() {
  loading.value = true
  try {
    const res = await getLowStock()
    tableData.value = res.data || []
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>
